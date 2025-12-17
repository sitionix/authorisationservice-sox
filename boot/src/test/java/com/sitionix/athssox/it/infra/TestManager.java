package com.sitionix.athssox.it.infra;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;
import com.sitionix.forgeit.postgresql.api.PostgresqlSupport;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

@ForgeFeatures(value = {
        MockMvcSupport.class,
        WireMockSupport.class,
        PostgresqlSupport.class
})
public interface TestManager extends ForgeIT {
}
