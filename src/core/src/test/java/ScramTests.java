import com.edgedb.exceptions.ScramException;
import com.edgedb.util.Scram;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

public class ScramTests {
    private static final String SCRAM_USERNAME = "user";
    private static final String SCRAM_PASSWORD = "pencil";
    private static final String SCRAM_CLIENT_NONCE = "rOprNGfwEbeRWgbNEkqO";
    private static final String SCRAM_SERVER_NONCE = "rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0";
    private static final String SCRAM_SALT = "W22ZaJ0SNY7soEsUEjb6gQ==";

    private Scram createScram() {
        return new Scram(Base64.getDecoder().decode(SCRAM_CLIENT_NONCE));
    }

    @Test
    public void testScramFlow() throws ScramException {
        var scram = createScram();

        var clientFirst = scram.buildInitialMessage(SCRAM_USERNAME);

        assertThat(clientFirst).isEqualTo(String.format("n,,n=%s,r=%s", SCRAM_USERNAME, SCRAM_CLIENT_NONCE));

        var serverFirst = String.format("r=%s,s=%s,i=4096", SCRAM_SERVER_NONCE, SCRAM_SALT);

        var clientFinal = scram.buildFinalMessage(serverFirst, SCRAM_PASSWORD);

        assertThat(clientFinal.message).isEqualTo(String.format("c=biws,r=%s,p=dHzbZapWIk4jUhN+Ute9ytag9zjfMHgsqmmiz7AndVQ=", SCRAM_SERVER_NONCE));
        assertThat(Base64.getEncoder().encodeToString(clientFinal.signature)).isEqualTo("6rriTRBi23WpRR/wtup+mMhUZUn/dB5nLTJRsjl95G4=");
    }

}
