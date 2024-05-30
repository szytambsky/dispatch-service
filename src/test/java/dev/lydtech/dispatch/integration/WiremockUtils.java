package dev.lydtech.dispatch.integration;

import com.github.tomakehurst.wiremock.client.WireMock;

public class WiremockUtils {

    public static void reset() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.resetAllScenarios();
        WireMock.resetToDefault();
    }

    public static void stubWiremock(String url, int httpStatusResponse, String body) {
        stubWiremock(url, httpStatusResponse, body, null, null, null);
    }

    public static void stubWiremock(String url, int httpStatusResponse,
                                    String body, String scenario,
                                    String initialState, String nextState) {
        if (scenario != null) {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(url))
                    .inScenario(scenario)
                    .whenScenarioStateIs(initialState)
                    .willReturn(WireMock.aResponse().withStatus(httpStatusResponse).withHeader("Content-Type", "text/plain").withBody(body))
                    .willSetStateTo(nextState));
        } else {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(url))
                    .willReturn(WireMock.aResponse().withStatus(httpStatusResponse).withHeader("Content-Type", "text/plain").withBody(body)));
        }

    }
}
