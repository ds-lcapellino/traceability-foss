/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the CatenaX (ng) GitHub Organisation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

import { CalendarDateModel } from '@core/model/calendar-date.model';
import { Notification, Notifications, NotificationStatus } from '@shared/model/notification.model';
import { View } from '@shared/model/view.model';
import { SharedModule } from '@shared/shared.module';
import { TemplateModule } from '@shared/template.module';
import { fireEvent, screen, waitFor, within } from '@testing-library/angular';
import { renderComponent } from '@tests/test-render.utils';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { buildMockInvestigations } from '../../../../../mocks/services/investigations-mock/investigations.model';
import { NotificationModule } from '../notification.module';

describe('NotificationsInboxComponent', () => {
  let clickHandler;

  beforeEach(() => (clickHandler = jest.fn()));

  const renderNotificationsInbox = () => {
    const qContent = buildMockInvestigations([NotificationStatus.CREATED]).map(data => {
      return { ...data, createDate: new CalendarDateModel(data.createDate) };
    });
    const qarContent = buildMockInvestigations([NotificationStatus.RECEIVED]).map(data => {
      return { ...data, createDate: new CalendarDateModel(data.createDate) };
    });

    const queuedAndRequestedNotifications$: Observable<View<Notifications>> = of({
      data: { content: qContent, page: 0, pageCount: 1, pageSize: 5, totalItems: 1 },
    }).pipe(delay(0));

    const receivedNotifications$: Observable<View<Notifications>> = of({
      data: { content: qarContent, page: 0, pageCount: 1, pageSize: 5, totalItems: 1 },
    }).pipe(delay(0));

    return renderComponent(
      `<app-notification 
          [queuedAndRequestedNotifications$]='queuedAndRequestedNotifications$'
          [receivedNotifications$]='receivedNotifications$'
          [translationContext]="'pageInvestigations'"

          (onReceivedPagination)='clickHandler($event)'
          (onQueuedAndRequestedPagination)='clickHandler($event)'
        ></app-notification>`,
      {
        imports: [SharedModule, NotificationModule, TemplateModule],
        translations: ['common'],
        componentProperties: { queuedAndRequestedNotifications$, receivedNotifications$, clickHandler },
      },
    );
  };

  it('should render received notifications', async () => {
    const component = await renderNotificationsInbox();
    component.detectChanges();
    expect(await screen.findByText('Investigation No 1')).toBeInTheDocument();
  });

  it('should render received notifications with date and status', async () => {
    await renderNotificationsInbox();

    const descriptionEl = await screen.findByText('Investigation No 1');
    const row = descriptionEl.closest('tr');

    expect(within(row).getByText('5/1/2022')).toBeInTheDocument();
    expect(within(row).getByText('Received')).toBeInTheDocument();
  });

  it('should be able to change notifications page', async () => {
    await renderNotificationsInbox();

    await screen.findByText('Investigation No 1');
    fireEvent.click(screen.getByLabelText('Next page'));

    expect(await screen.findByText('Investigation No 6')).toBeInTheDocument();
  });

  it('should render queued & requested notifications', async () => {
    await renderNotificationsInbox();
    fireEvent.click(screen.getByText('Queued & Requested'));
    expect(await screen.findByText('Investigation No 1')).toBeInTheDocument();
  });
});
