package com.creatoros.social;

import com.creatoros.exception.ConflictException;
import com.creatoros.exception.ResourceNotFoundException;
import com.creatoros.social.dto.ConnectSocialAccountRequest;
import com.creatoros.social.integration.PlatformProfile;
import com.creatoros.social.integration.SocialConnectRequest;
import com.creatoros.social.integration.SocialPlatformClientRegistry;
import com.creatoros.user.User;
import com.creatoros.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final SocialPlatformClientRegistry clientRegistry;

    public SocialAccountService(SocialAccountRepository socialAccountRepository,
                                 UserRepository userRepository,
                                 SocialPlatformClientRegistry clientRegistry) {
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.clientRegistry = clientRegistry;
    }

    @Transactional(readOnly = true)
    public List<SocialAccount> listForUser(UUID userId) {
        return socialAccountRepository.findByUserId(userId);
    }

    @Transactional
    public SocialAccount connect(UUID userId, ConnectSocialAccountRequest request) {
        User user = userRepository.getReferenceById(userId);
        var client = clientRegistry.resolve(request.platform());
        PlatformProfile profile = client.fetchAccountProfile(
                new SocialConnectRequest(request.handle(), null, null));

        if (socialAccountRepository.existsByPlatformAndPlatformAccountId(request.platform(), profile.platformAccountId())) {
            throw new ConflictException("ACCOUNT_ALREADY_CONNECTED",
                    "This " + request.platform() + " account is already connected");
        }

        SocialAccount account = SocialAccount.builder()
                .user(user)
                .platform(request.platform())
                .platformAccountId(profile.platformAccountId())
                .username(profile.username())
                .connectedAt(Instant.now())
                .status(SocialAccountStatus.CONNECTED)
                .build();
        return socialAccountRepository.save(account);
    }

    @Transactional
    public void disconnect(UUID userId, UUID accountId) {
        SocialAccount account = socialAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SocialAccount", accountId));
        socialAccountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public SocialAccount requireOwned(UUID userId, UUID accountId) {
        return socialAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SocialAccount", accountId));
    }
}
