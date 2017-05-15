package org.apache.tez.tools.debug;

import java.util.List;
import java.util.Set;

import org.apache.tez.tools.debug.Params.Param;

public interface ArtifactSource {
  Set<Param> getRequiredParams();
  Set<Param> getProvidedParams();
  List<Artifact> getArtifacts(Params params);
}