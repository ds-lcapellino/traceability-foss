/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.traceability.assets.application.rest.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.eclipse.tractusx.traceability.assets.domain.model.SemanticModel;

import java.time.Instant;

@Builder
@Data
public class SemanticModelResponse {
    @ApiModelProperty(example = "2022-02-04T13:48:54Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant manufacturingDate;
    @ApiModelProperty(example = "DEU")
    @Size(max = 255)
    private String manufacturingCountry;
    @ApiModelProperty(example = "33740332-54")
    @Size(max = 255)
    private String manufacturerPartId;
    @ApiModelProperty(example = "33740332-54")
    @Size(max = 255)
    private String customerPartId;
    @ApiModelProperty(example = "Door f-r")
    @Size(max = 255)
    private String nameAtManufacturer;
    @ApiModelProperty(example = "Door front-right")
    @Size(max = 255)
    private String nameAtCustomer;

    public static SemanticModelResponse from(final SemanticModel semanticModel) {
        return SemanticModelResponse.builder()
                .customerPartId(semanticModel.getCustomerPartId())
                .manufacturerPartId(semanticModel.getManufacturerPartId())
                .manufacturingCountry(semanticModel.getManufacturingCountry())
                .manufacturingDate(semanticModel.getManufacturingDate())
                .nameAtCustomer(semanticModel.getNameAtCustomer())
                .nameAtManufacturer(semanticModel.getNameAtManufacturer())
                .build();
    }
}