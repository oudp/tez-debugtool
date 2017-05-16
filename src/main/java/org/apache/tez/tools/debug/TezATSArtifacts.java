package org.apache.tez.tools.debug;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.tez.tools.debug.Params.Param;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class TezATSArtifacts implements ArtifactSource {

  private final ATSArtifactHelper helper;

  @Inject
  public TezATSArtifacts(ATSArtifactHelper helper) {
    this.helper = helper;
  }

  @Override
  public Set<Param> getRequiredParams() {
    return Collections.singleton(Param.TEZ_DAG_ID);
  }

  @Override
  public List<Artifact> getArtifacts(Params params) {
    String dagId = params.getParam(Param.TEZ_DAG_ID);
    try {
      return ImmutableList.of(
          helper.getEntityArtifact("TEZ_DAG", "TEZ_DAG_ID", dagId),
          helper.getEntityArtifact("TEZ_DAG_EXTRAINFO", "TEZ_DAG_EXTRA_INFO", dagId),
          helper.getChildEntityArtifact("TEZ_VERTEX", "TEZ_VERTEX_ID", "TEZ_DAG_ID", dagId),
          helper.getChildEntityArtifact("TEZ_TASK", "TEZ_TASK_ID", "TEZ_DAG_ID", dagId),
          helper.getChildEntityArtifact("TEZ_TASK_ATTEMPT", "TEZ_TASK_ATTEMPT_ID", "TEZ_DAG_ID",
              dagId));
    } catch (URISyntaxException e) {
      // This should go back to user.
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void updateParams(Params param, Artifact artifact, Path path) throws IOException {
    if (param.getParam(Param.HIVE_QUERY_ID) != null &&
        param.getParam(Param.TEZ_APP_ID) != null) {
      return;
    }
    if (artifact.getName().equals("TEZ_DAG")) {
      InputStream stream = Files.newInputStream(path);
      JsonNode node = new ObjectMapper().readTree(stream);
      if (node == null) {
        return;
      }
      JsonNode other = node.get("otherinfo");
      if (other == null) {
        return;
      }
      // Get and update dag id/hive query id.
      if (param.getParam(Param.TEZ_APP_ID) == null) {
        JsonNode appId = other.get("applicationId");
        if (appId != null && appId.isTextual()) {
          param.setParam(Param.TEZ_APP_ID, appId.asText());
        }
      }
      if (param.getParam(Param.HIVE_QUERY_ID) == null) {
        JsonNode callerType = other.get("callerType");
        if (callerType != null && callerType.isTextual() &&
            callerType.asText().equals("HIVE_QUERY_ID")) {
          JsonNode callerId = other.get("callerId");
          if (callerId != null && callerId.isTextual()) {
            param.setParam(Param.HIVE_QUERY_ID, callerId.asText());
          }
        }
      }
    }
  }
}
