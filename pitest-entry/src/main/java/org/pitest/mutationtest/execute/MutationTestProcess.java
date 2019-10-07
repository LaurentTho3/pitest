package org.pitest.mutationtest.execute;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

import org.pitest.mutationtest.MutationStatusMap;
import org.pitest.mutationtest.MutationStatusTestPair;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.process.ProcessArgs;
import org.pitest.process.WrappingProcess;
import org.pitest.util.ExitCode;

public class MutationTestProcess {

  private final WrappingProcess                 process;
  private final MutationTestCommunicationThread thread;

  public MutationTestProcess(final ServerSocket socket,
      final ProcessArgs processArgs, final MinionArguments arguments, final boolean enableAdamu) {
    this.process = new WrappingProcess(socket.getLocalPort(), processArgs,
        MutationTestMinion.class, enableAdamu);
    this.thread = new MutationTestCommunicationThread(socket, arguments,
        new HashMap<MutationIdentifier, MutationStatusTestPair>());

  }

  public void start() throws IOException, InterruptedException {
    this.thread.start();
    this.process.start();
  }

  public MutationStatusTestPair results(MutationDetails mutant) {
    return this.thread.getStatus(mutant.getId());
  }

  public void results(final MutationStatusMap allmutations) throws IOException {
    this.results(allmutations, false);
  }

  public void results(final MutationStatusMap allmutations, boolean enableAdamu) throws IOException {

    for (final MutationDetails each : allmutations.allMutations()) {
      final MutationStatusTestPair status = this.thread.getStatus(each.getId());
      if (status != null) {
        allmutations.setStatusForMutation(each, status);
        if (enableAdamu) {
          jp.mzw.adamu.adaptation.Monitor.monitorMutationResult(each, status.getStatus());
        }
      }
    }

  }

  public ExitCode waitToDie() {
    try {
      return this.thread.waitToFinish();
    } finally {
      this.process.destroy();
    }

  }

}
