/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

export enum QualityAlertFlow {
  BOTTOM_UP = 'BOTTOM-UP',
  TOP_DOWN = 'TOP-DOWN',
}

export enum QualityTypes {
  MINOR = 'rgba(255, 199, 31, 0.6)',
  MAJOR = 'rgba(254, 103, 2, 0.6)',
  CRITICAL = '#c9585a',
  'LIFE-THREATENING' = '#905680',
}

export enum QualityAlertIcons {
  MINOR = 'error-warning-line',
  MAJOR = 'alert-line',
  CRITICAL = 'spam-line',
  'LIFE-THREATENING' = 'close-circle-line',
}

export enum QualityAlertTypes {
  PENDING = 'pending',
  EXTERNAL = 'external',
  DISTRIBUTED = 'committed',
  CREATED = 'created',
}