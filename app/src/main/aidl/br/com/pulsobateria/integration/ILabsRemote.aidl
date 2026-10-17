package br.com.pulsobateria.integration;
import android.os.Bundle;
/** Contrato IPC v1; acrescente novos métodos no fim, sem alterar assinaturas. */
interface ILabsRemote {
    Bundle startTest(in Bundle request);
    Bundle stopTest(long testId);
    Bundle getStatus();
    Bundle getReport(long testId);
}
