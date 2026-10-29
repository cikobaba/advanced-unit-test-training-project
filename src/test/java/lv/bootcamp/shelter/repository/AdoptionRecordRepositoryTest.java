package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.AdoptionRecord;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AdoptionRecordRepositoryTest {

    @Autowired
    private AdoptionRecordRepository adoptionRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findRecentAdoptionsByAnimalType_shouldFilterByTypeAndDate() {
        LocalDateTime since = LocalDateTime.of(2026, 10, 1, 0, 0);

        Animal recentDog = saveAnimal("Rex", AnimalType.DOG);
        Animal oldDog = saveAnimal("Max", AnimalType.DOG);
        Animal recentCat = saveAnimal("Mia", AnimalType.CAT);
        Animal boundaryDog = saveAnimal("Buddy", AnimalType.DOG);

        saveRecord(recentDog, "anna@example.com", since.plusDays(1));
        saveRecord(oldDog, "bob@example.com", since.minusDays(1));
        saveRecord(recentCat, "mia@example.com", since.plusDays(1));
        saveRecord(boundaryDog, "john@example.com", since);

        entityManager.flush();
        entityManager.clear();

        var results = adoptionRecordRepository
                .findRecentAdoptionsByAnimalType(AnimalType.DOG, since);

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(AdoptionRecord::getAdopterEmail)
                .containsExactlyInAnyOrder(
                        "anna@example.com",
                        "john@example.com"
                );
    }

    private Animal saveAnimal(String name, AnimalType type) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(AnimalStatus.ADOPTED);

        entityManager.persist(animal);
        return animal;
    }

    private void saveRecord(
            Animal animal,
            String email,
            LocalDateTime adoptedAt
    ) {
        AdoptionRecord record = new AdoptionRecord();
        record.setAnimal(animal);
        record.setAdopterEmail(email);
        record.setAdoptedAt(adoptedAt);

        entityManager.persist(record);
    }
}
