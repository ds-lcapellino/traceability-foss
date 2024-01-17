/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.traceability.assets.domain.importpoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.tractusx.irs.edc.client.policy.AcceptedPoliciesProvider;
import org.eclipse.tractusx.irs.edc.client.policy.AcceptedPolicy;
import org.eclipse.tractusx.irs.edc.client.policy.Policy;
import org.eclipse.tractusx.traceability.assets.domain.asbuilt.repository.AssetAsBuiltRepository;
import org.eclipse.tractusx.traceability.assets.domain.asplanned.repository.AssetAsPlannedRepository;
import org.eclipse.tractusx.traceability.assets.domain.importpoc.repository.SubmodelPayloadRepository;
import org.eclipse.tractusx.traceability.common.model.BPN;
import org.eclipse.tractusx.traceability.common.properties.TraceabilityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportServiceImplTest {

    @InjectMocks
    private ImportServiceImpl importService;

    @Mock
    private AssetAsPlannedRepository assetAsPlannedRepository;
    @Mock
    private AssetAsBuiltRepository assetAsBuiltRepository;
    @Mock
    private SubmodelPayloadRepository submodelPayloadRepository;

    private MappingStrategyFactory strategyFactory;
    @Mock
    private TraceabilityProperties traceabilityProperties;
    @Mock
    private AcceptedPoliciesProvider acceptedPoliciesProvider;


    @BeforeEach
    public void testSetup() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        importService = new ImportServiceImpl(objectMapper, assetAsPlannedRepository, assetAsBuiltRepository, traceabilityProperties, new MappingStrategyFactory(), submodelPayloadRepository,acceptedPoliciesProvider);

    }

    @Test
    void testImportRequestSuccessful() throws IOException {

        InputStream file = ImportServiceImplTest.class.getResourceAsStream("/testdata/import-request.json");
        // Convert the file to a MockMultipartFile
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",               // Parameter name in the multipart request
                "import-request",             // Original file name
                "application/json",   // Content type
                file
        );

        when(traceabilityProperties.getBpn()).thenReturn(BPN.of("BPNL00000003CML1"));
        importService.importAssets(multipartFile);
        verify(assetAsBuiltRepository, times(1)).saveAllIfNotInIRSSyncAndUpdateImportStateAndNote(anyList());
        verify(assetAsPlannedRepository, times(1)).saveAllIfNotInIRSSyncAndUpdateImportStateAndNote(anyList());
    }


    @Test
    void testGetPolicyByID() {


        // GIVEN
        String policyId = "policy123";
        OffsetDateTime createdOn = OffsetDateTime.parse("2023-07-03T16:01:05.309Z");
        List<AcceptedPolicy> acceptedPolicies = List.of(
                new AcceptedPolicy(new Policy("policy123", createdOn,null, null),null));

        // WHEN
        when(acceptedPoliciesProvider.getAcceptedPolicies()).thenReturn(acceptedPolicies);
        List<Policy> result = importService.getAllPolicies();

        // THEN
        assertNotNull(result);
        assertEquals(policyId, result.get(0).getPolicyId());
        assertEquals(createdOn, result.get(0).getCreatedOn());
    }



}
