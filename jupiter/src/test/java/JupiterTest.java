import com.mmorrell.jupiter.model.JupiterPerpPosition;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Base58;
import org.junit.jupiter.api.Test;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.AccountInfo;
import org.p2p.solanaj.rpc.types.Memcmp;
import org.p2p.solanaj.rpc.types.ProgramAccount;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Jupiter Perpetuals positions.
 */
@Slf4j
public class JupiterTest {

    private final RpcClient client = new RpcClient("https://mainnet.helius-rpc.com/?api-key=a778b653-bdd6-41bc-8cda-0c7377faf1dd");

    @Test
    public void testJupiterPerpPositionDeserialization() throws RpcException {
        PublicKey positionPublicKey = new PublicKey("FdqbJAvADUJzZsBFK1ArhV79vXLmpKUMB4oXSrW8rSE");
        PublicKey positionPublicKeyOwner = new PublicKey("skynetDj29GH6o6bAqoixCpDuYtWqi1rm8ZNx1hB3vq");

        // Fetch the account data
        AccountInfo accountInfo = client.getApi().getAccountInfo(positionPublicKey);

        assertNotNull(accountInfo, "Account info should not be null");

        byte[] data = Base64.getDecoder().decode(accountInfo.getValue().getData().get(0));

        // Deserialize the data into JupiterPerpPosition
        JupiterPerpPosition position = JupiterPerpPosition.fromByteArray(data);

        // Log the deserialized position
        log.info("Deserialized JupiterPerpPosition: {}", position);

        // Assertions
        assertNotNull(position);
        assertEquals(positionPublicKeyOwner, position.getOwner());

        // Add more specific assertions based on expected values
        assertNotNull(position.getPool());
        assertNotNull(position.getCustody());
        assertNotNull(position.getCollateralCustody());
        assertTrue(position.getOpenTime() > 0);
        assertTrue(position.getUpdateTime() > 0);
        assertNotNull(position.getSide());
        assertTrue(position.getPrice() > 0);
        assertTrue(position.getSizeUsd() > 0);
        assertTrue(position.getCollateralUsd() > 0);
        // Add more assertions as needed
    }

    @Test
    public void testGetAllJupiterPerpPositions() throws RpcException {
        PublicKey programId = new PublicKey("PERPHjGBqRHArX4DySjwM6UJHiR3sWAatqfdBS2qQJu");

        // Get the discriminator for the Position account
        byte[] positionDiscriminator = getAccountDiscriminator("Position");

        // Create a memcmp filter for the discriminator at offset 0
        Memcmp memcmpFilter = new Memcmp(0, Base58.encode(positionDiscriminator));

        // Get all program accounts matching the filters
        List<ProgramAccount> positionAccounts = client.getApi().getProgramAccounts(
                programId,
                Collections.singletonList(memcmpFilter),
                216
        );

        List<JupiterPerpPosition> positions = new ArrayList<>();
        for (ProgramAccount account : positionAccounts) {
            // Decode the account data
            byte[] data = account.getAccount().getDecodedData();

            // Deserialize the data into JupiterPerpPosition
            JupiterPerpPosition position = JupiterPerpPosition.fromByteArray(data);

            if (position.getSizeUsd() > 0) {
                // Add to the list
                positions.add(position);
            }
        }

        positions.sort(Comparator.comparingLong(JupiterPerpPosition::getSizeUsd));

        // Log the positions
        for (JupiterPerpPosition position : positions) {
            double leverage = (double) position.getSizeUsd() / position.getCollateralUsd();
            log.info("Owner: {}, Size USD: {}, Leverage: {}", position.getOwner().toBase58(), position.getSizeUsd(), leverage);
        }
    }

    /**
     * Calculates the account discriminator for a given account name.
     *
     * @param accountName the name of the account.
     * @return the first 8 bytes of the SHA-256 hash of "account:<accountName>".
     */
    private byte[] getAccountDiscriminator(String accountName) {
        String preimage = "account:" + accountName;
        try {
            MessageDigest hasher = MessageDigest.getInstance("SHA-256");
            hasher.update(preimage.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOfRange(hasher.digest(), 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found for discriminator calculation.");
        }
    }
}
