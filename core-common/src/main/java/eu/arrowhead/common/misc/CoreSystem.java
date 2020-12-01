/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.misc;

import eu.arrowhead.common.ArrowheadMain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CoreSystem {
  AUTHORIZATION(8444, 8445, Arrays.asList("db_user", "db_password", "db_address", "keystore", "keystorepass"),
                Arrays.asList("keypass", "truststore", "truststorepass"),
                Arrays.asList(CoreSystemService.AUTH_CONTROL_SERVICE, CoreSystemService.TOKEN_GEN_SERVICE), 62541),
  CERTIFICATE_AUTHORITY(8458, 8459, null,
                        Arrays.asList("keystore", "keystorepass", "keypass", "truststore", "truststorepass", "cloudstore", "cloudstorepass"), null, 62542),
  CHOREOGRAPHER(8456, 8457, null, null, null,62543),
  EVENT_HANDLER(8454, 8455, ArrowheadMain.dbFields, ArrowheadMain.certFields,
                Arrays.asList(CoreSystemService.EVENT_PUBLISH, CoreSystemService.EVENT_SUBSCRIPTION), 62544),
  GATEKEEPER_INTERNAL(8446, 8447, ArrowheadMain.dbFields, Arrays
      .asList("gatekeeper_keystore", "gatekeeper_keystore_pass", "gatekeeper_keypass", "cloud_keystore", "cloud_keystore_pass", "cloud_keypass",
              "master_arrowhead_cert"), null,62545),
  GATEKEEPER_EXTERNAL(8448, 8449, ArrowheadMain.dbFields, Arrays
      .asList("gatekeeper_keystore", "gatekeeper_keystore_pass", "gatekeeper_keypass", "cloud_keystore", "cloud_keystore_pass", "cloud_keypass",
              "master_arrowhead_cert"), Arrays.asList(CoreSystemService.GSD_SERVICE, CoreSystemService.ICN_SERVICE),62546),
  GATEWAY(8452, 8453, null,
          Arrays.asList("keystore", "keystorepass", "keypass", "truststore", "truststorepass", "trustpass", "master_arrowhead_cert"),
          Arrays.asList(CoreSystemService.GW_PROVIDER_SERVICE, CoreSystemService.GW_CONSUMER_SERVICE, CoreSystemService.GW_SESSION_MGMT), 62547),
  ORCHESTRATOR(8440, 8441, ArrowheadMain.dbFields, ArrowheadMain.certFields, Collections.singletonList(CoreSystemService.ORCH_SERVICE), 62548),
  QOS(8450, 8451, Arrays.asList("db_user", "db_password", "db_address", "monitor_url"), ArrowheadMain.certFields, null, 62549),
  SERVICE_REGISTRY_DNS(8442, 8443, null, ArrowheadMain.certFields, null, 62550),
  SERVICE_REGISTRY_SQL(8442, 8443, ArrowheadMain.dbFields, ArrowheadMain.certFields,
                       Arrays.asList(CoreSystemService.SERVICE_LOOKUP_SERVICE, CoreSystemService.SERVICE_REGISTRY_SERVICE), 62551),
  SYSTEM_REGISTRY(8436, 8437, ArrowheadMain.dbFields, ArrowheadMain.certFields, Collections.singletonList(CoreSystemService.SYSTEM_REGISTRY_SERVICE), 62552),
  DEVICE_REGISTRY(8438, 8439, ArrowheadMain.dbFields, ArrowheadMain.certFields,
                  Collections.singletonList(CoreSystemService.DEVICE_REGISTRY_SERVICE), 62553),
  ONBOARDING(8434, 8435, ArrowheadMain.dbFields, ArrowheadMain.certFields,
             Collections.singletonList(CoreSystemService.ONBOARDING_SERVICE), 62554);

	private final int insecurePort;
	private final int securePort;
	private final int opcuaPort;
	private final List<String> alwaysMandatoryFields;
	private final List<String> secureMandatoryFields;
	private final List<CoreSystemService> services;

	CoreSystem(int insecPort, int secPort, List<String> awf, List<String> smf, List<CoreSystemService> cs,
			int opcuaPort) {
		insecurePort = insecPort;
		securePort = secPort;
		if (awf == null) {
			alwaysMandatoryFields = new ArrayList<>();
		} else {
			alwaysMandatoryFields = awf;
		}
		if (smf == null) {
			secureMandatoryFields = new ArrayList<>();
		} else {
			secureMandatoryFields = smf;
		}
		if (cs == null) {
			services = new ArrayList<>();
		} else {
			services = cs;
		}
		this.opcuaPort = opcuaPort;
	}

	public int getInsecurePort() {
		return insecurePort;
	}

	public int getSecurePort() {
		return securePort;
	}

	public List<String> getAlwaysMandatoryFields() {
		return alwaysMandatoryFields;
	}

	public List<String> getSecureMandatoryFields() {
		return secureMandatoryFields;
	}

	public List<CoreSystemService> getServices() {
		return services;
	}

	public int getOpcUaPort() {
		return opcuaPort;
	}
}
