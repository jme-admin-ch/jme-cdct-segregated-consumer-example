package ch.admin.bit.jeap.jme.cdct.consumer.infrastructure.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Note: The consumer is not interested in all data of the provider's users. The consumer's users only
 * include id and name, but not the additional tag and the createdAt data from the provider's name.
 * Therefore, when specifying pacts for the provider, this consumer will completely ignore those additional fields,
 * because those are not his business.
 */
@Data
@NoArgsConstructor
public class User {

    @NonNull
    private String id;

    @NonNull
    private String name;

}
