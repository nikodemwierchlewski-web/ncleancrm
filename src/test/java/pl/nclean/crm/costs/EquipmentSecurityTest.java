package pl.nclean.crm.costs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipmentController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class EquipmentSecurityTest {
    @MockitoBean pl.nclean.crm.costs.DepreciationService depreciation;
 @Autowired MockMvc mvc;
    @MockitoBean EquipmentService service;
    @Test void blankOptionalFieldsAndUncheckedHistoryBindToCommand() throws Exception {
        mvc.perform(post("/equipment/asset").with(user("owner").roles("OWNER")).with(csrf())
                .param("token",java.util.UUID.randomUUID().toString()).param("id","").param("version","0")
                .param("name","Ekstraktor QA").param("kind","EQUIPMENT").param("active","true")
                .param("category","").param("model","").param("serial","").param("supplier","").param("note","")
                .param("purchasedOn","").param("startedOn","").param("warrantyUntil","").param("historyBoundary","")
                .param("_historyComplete","on"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
    }
    @Test void monthAndEnabledFlagBindForEconomicPlan() throws Exception {
        mvc.perform(post("/equipment/depreciation").with(user("owner").roles("OWNER")).with(csrf())
                .param("assetId","1").param("version","-1").param("token",java.util.UUID.randomUUID().toString())
                .param("initialValue","3600.00").param("residualValue","0.00").param("startMonth","2026-01")
                .param("months","36").param("_enabled","on").param("enabled","true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        var command=org.mockito.ArgumentCaptor.forClass(DepreciationService.Command.class);
        org.mockito.Mockito.verify(depreciation).save(command.capture(),org.mockito.ArgumentMatchers.eq("owner"));
        org.junit.jupiter.api.Assertions.assertEquals(java.time.YearMonth.of(2026,1),command.getValue().startMonth());
        org.junit.jupiter.api.Assertions.assertTrue(command.getValue().enabled());
    }
    @Test void readsAndWritesRequireOwnerAndCsrf() throws Exception {
        for(String path:new String[]{"/equipment","/equipment/1","/equipment/editor?type=asset"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        }
        for(String path:new String[]{"asset","archive","counter","reading","plan","execute","snooze","cancel","depreciation"}) {
            mvc.perform(post("/equipment/"+path).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());
            mvc.perform(post("/equipment/"+path).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
        }
    }
}
