/********************************************************************************
 * Copyright (c) 2022, 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2022, 2023 ZF Friedrichshafen AG
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.traceability.infrastructure.edc.blackbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.offer.ContractOffer;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.v4.model.CatalogItem;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.v4.model.ContractOfferDescription;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.v4.model.NegotiationRequest;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.v4.model.NegotiationResponse;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.v4.model.Response;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.v4.model.TransferProcessRequest;
import org.eclipse.tractusx.traceability.infrastructure.edc.blackbox.v4.transformer.EdcTransformer;
import org.eclipse.tractusx.traceability.infrastructure.edc.properties.EdcProperties;
import org.eclipse.tractusx.traceability.qualitynotification.domain.model.QualityNotificationMessage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class EdcService {

    public static final MediaType JSON = MediaType.get("application/json");

    private final HttpCallService httpCallService;
    private final ObjectMapper objectMapper;
    private final EdcProperties edcProperties;
    public static final String EDC_PROTOCOL = "dataspace-protocol-http";
    private final EdcTransformer edcTransformer;

    /**
     * Rest call to get all contract offer and filter notification type contract
     */
    public Catalog getCatalog(
            String consumerEdcDataManagementUrl,
            String providerConnectorControlPlaneIDSUrl,
            Map<String, String> header,
            QualityNotificationMessage qualityNotificationMessage
    ) throws IOException {

        String notificationType = qualityNotificationMessage.getType().toString().toLowerCase();
        String method = qualityNotificationMessage.getIsInitial() ? "receive" : "update";

        Catalog catalog = httpCallService.getCatalogForNotification(consumerEdcDataManagementUrl, providerConnectorControlPlaneIDSUrl, notificationType, method, header);

        if (catalog.getDatasets().isEmpty()) {
            log.error("No contract found");
            throw new BadRequestException("Provider has no contract offers for us. Catalog is empty.");
        }

    /*    catalog.getDatasets().stream().filter(dataset -> {

        })*/
        catalog.getDatasets().forEach(dataset -> {
            dataset.getProperties().values().forEach(o -> {
                if (o != null) {
                    log.info("Dataset {}", o.toString());
                }
            });
            dataset.getProperties().forEach((s, o) -> {
                log.info("key {}, value {}", s, o.toString());
            });

            if (dataset.getProperty("edc:notificationtype") != null) {
                log.info("Dataset1 notificationType {}", dataset.getProperty("edc:notificationtype").toString());
            }

            if (dataset.getProperty("notificationtype") != null) {
                log.info("Dataset2 notificationType {}", dataset.getProperty("edc:notificationtype").toString());
            }

        });

        return catalog;
    }


    private boolean hasTracePolicy(ContractOffer contractOffer) {
        return contractOffer.getPolicy() != null && contractOffer.getPolicy().hasTracePolicy();
    }

    /**
     * Prepare for contract negotiation. it will wait for while till API return agreementId
     */
    public String initializeContractNegotiation(String providerConnectorUrl, CatalogItem catalogItem, String consumerEdcUrl,
                                                Map<String, String> header) throws InterruptedException, IOException {

        final NegotiationRequest negotiationRequest = createNegotiationRequestFromCatalogItem(providerConnectorUrl + edcProperties.getIdsPath(),
                catalogItem);


        log.info(":::: Start Contract Negotiation method[initializeContractNegotiation] offerId :{}, assetId:{}", negotiationRequest.getOffer().getOfferId(), negotiationRequest.getOffer().getAssetId());

        String negotiationId = initiateNegotiation(negotiationRequest, consumerEdcUrl, header);
        NegotiationResponse negotiation = null;

        // Check negotiation state
        while (negotiation == null || negotiation.getState() == null || !negotiation.getState().equals("FINALIZED")) {

            log.info(":::: waiting for contract to get confirmed");
            if (negotiation != null) {
                log.info("Negotation state {}", negotiation.getState());
            }

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            ScheduledFuture<NegotiationResponse> scheduledFuture =
                    scheduler.schedule(() -> {
                        var url = consumerEdcUrl + edcProperties.getNegotiationPath() + "/" + negotiationId;
                        var request = new Request.Builder().url(url);
                        header.forEach(request::addHeader);

                        log.info(":::: Start call for contract agreement method [initializeContractNegotiation] URL :{}", url);

                        return httpCallService.sendRequestNegotiation(request.build());
                    }, 1000, TimeUnit.MILLISECONDS);
            try {
                negotiation = scheduledFuture.get();
                scheduler.shutdown();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } finally {
                if (!scheduler.isShutdown()) {
                    scheduler.shutdown();
                }
            }
        }
        return negotiation.getContractAgreementId();
    }


    private NegotiationRequest createNegotiationRequestFromCatalogItem(final String providerConnectorUrl,
                                                                       final CatalogItem catalogItem) {
        final var contractOfferDescription = ContractOfferDescription.builder()
                .offerId(catalogItem.getOfferId())
                .assetId(catalogItem.getPolicy().getTarget())
                .policy(catalogItem.getPolicy())
                .build();
        NegotiationRequest request = NegotiationRequest.builder()
                .connectorId(catalogItem.getConnectorId())
                .connectorAddress(providerConnectorUrl)
                .protocol(EDC_PROTOCOL)
                .offer(contractOfferDescription)
                .build();
        log.info("NegotiationRequest {}", edcTransformer.transformNegotiationRequestToJson(request).toString());
        return request;
    }

    /**
     * Rest call for Contract negotiation and return agreementId.
     */
    private String initiateNegotiation(NegotiationRequest negotiationRequest, String consumerEdcDataManagementUrl,
                                       Map<String, String> headers) throws IOException {
        var url = consumerEdcDataManagementUrl + edcProperties.getNegotiationPath();

        final String jsonObject = edcTransformer.transformNegotiationRequestToJson(negotiationRequest).toString();
        var requestBody = RequestBody.create(jsonObject, JSON);
        var request = new Request.Builder().url(url).post(requestBody);

        headers.forEach(request::addHeader);
        Response negotiationIdResponse = (Response) httpCallService.sendRequest(request.build(), Response.class);
        log.info(":::: Method [initiateNegotiation] Negotiation Id :{}", negotiationIdResponse.getResponseId());
        return negotiationIdResponse.getResponseId();
    }


    /**
     * Rest call for Transfer Data with HttpProxy
     */
    public Response initiateHttpProxyTransferProcess(String consumerEdcDataManagementUrl,
                                                     String providerConnectorControlPlaneIDSUrl,
                                                     TransferProcessRequest transferProcessRequest,
                                                     Map<String, String> headers) throws IOException {
        var url = consumerEdcDataManagementUrl + edcProperties.getTransferPath();


        final String jsonObject = edcTransformer.transformTransferProcessRequestToJson(transferProcessRequest).toString();
        log.info("transferprocess json {}", jsonObject);
        var requestBody = RequestBody.create(jsonObject, JSON);
        var request = new Request.Builder().url(url).post(requestBody);

        headers.forEach(request::addHeader);
        log.info(":::: call Transfer1 process with http Proxy method[initiateHttpProxyTransferProcess] agreementId:{} ,assetId :{},consumerEdcDataManagementUrl :{}, providerConnectorControlPlaneIDSUrl:{}", transferProcessRequest.getContractId(), transferProcessRequest.getAssetId(), consumerEdcDataManagementUrl, providerConnectorControlPlaneIDSUrl);
        return (Response) httpCallService.sendRequest(request.build(), Response.class);
    }

    private boolean isQualityInvestigation(Dataset dataset) {
        Object notificationtype = dataset.getProperty("notificationtype");
        if (notificationtype != null) {
            return "qualityinvestigation".equals(notificationtype.toString());
        }
        return false;
    }

    private boolean isQualityAlert(Dataset dataset) {
        Object notificationtype = dataset.getProperty("notificationtype");
        if (notificationtype != null) {
            return "qualityalert".equals(notificationtype.toString());
        }
        return false;
    }
}
