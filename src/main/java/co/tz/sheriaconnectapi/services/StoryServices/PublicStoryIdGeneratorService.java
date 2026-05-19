package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PublicStoryIdGeneratorService {

    private final PublicStoryRepository publicStoryRepository;

    public PublicStoryIdGeneratorService(PublicStoryRepository publicStoryRepository) {
        this.publicStoryRepository = publicStoryRepository;
    }

    public String generate() {
        String publicId;
        do {
            publicId = "ST-" + UUID.randomUUID();
        } while (publicStoryRepository.existsByPublicId(publicId));

        return publicId;
    }
}
