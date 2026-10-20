package sg.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import sg.tracker.model.Disruption;
import sg.tracker.model.Severity;
import sg.tracker.service.DisruptionService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = sg.tracker.web.DisruptionController.class)
class DisruptionControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    DisruptionService service;

    private static Disruption sample() {
        return new Disruption("EWL|Both|EW27", "EWL", "Both", "EW27,EW26",
                "EWL: No train service.", Severity.MAJOR, true, Instant.parse("2026-10-20T00:00:00Z"));
    }

    @Test
    void listReturnsDisruptionsAsJson() throws Exception {
        when(service.find(null, true)).thenReturn(List.of(sample()));

        mvc.perform(get("/api/disruptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].line").value("EWL"))
                .andExpect(jsonPath("$[0].severity").value("MAJOR"))
                .andExpect(jsonPath("$[0].freeBusAvailable").value(true));
    }

    @Test
    void listPassesLineAndActiveOnlyFilters() throws Exception {
        when(service.find("EWL", false)).thenReturn(List.of());

        mvc.perform(get("/api/disruptions").param("line", "EWL").param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).find("EWL", false);
    }

    @Test
    void statusIsDisruptedWhenAnythingIsActive() throws Exception {
        when(service.activeCountByLine()).thenReturn(Map.of("EWL", 2L));

        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall").value("DISRUPTED"))
                .andExpect(jsonPath("$.activeCount").value(2))
                .andExpect(jsonPath("$.activeByLine.EWL").value(2));
    }

    @Test
    void statusIsNormalWhenNothingIsActive() throws Exception {
        when(service.activeCountByLine()).thenReturn(Map.of());

        mvc.perform(get("/api/status"))
                .andExpect(jsonPath("$.overall").value("NORMAL"))
                .andExpect(jsonPath("$.activeCount").value(0));
    }

    @Test
    void refreshTriggersServiceAndReturnsStatus() throws Exception {
        when(service.activeCountByLine()).thenReturn(Map.of());

        mvc.perform(post("/api/refresh")).andExpect(status().isOk());

        verify(service).refresh();
    }
}
