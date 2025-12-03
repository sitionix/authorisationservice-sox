package com.sitionix.athssox.it;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sitionix.athssox.entity.UserEntity;
import com.sitionix.athssox.infra.IntegrationTest;
import com.sitionix.athssox.util.TestResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.sitionix.athssox.util.CustomResultMatcher.jsonEqualIgnore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RegisterUserDOControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestResourceManager resourceManager;

    @BeforeEach
    public void setUp() {
        this.resourceManager.database().clean();
    }

    @DisplayName("should create new user in database")
    @Test
    @Disabled
    void givenUser_whenCreate_thenCreateNewUser() throws Exception {

        final String request = this.resourceManager.givenRequest().request("givenDefaultUserWithUserNameAndPassword.json");

        final String expected = this.resourceManager.expected().response("expectedUserResponseWhenCreateNewUser.json");

        final String url = "/api/v1/user";

        this.mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonEqualIgnore(expected, "id"))
                .andExpect(status().isCreated())
                .andReturn();

        final List<UserEntity> userEntities = this.resourceManager.database().getUsersTable().findAll();
        final List<UserEntity> expectedUserEntities = this.resourceManager.expected()
                .records("expectedUsersRecordWhenCreateUser.json", new TypeReference<>() {});

        assertThat(userEntities)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedUserEntities);

        assertThat(userEntities).hasSize(1);
    }
}
