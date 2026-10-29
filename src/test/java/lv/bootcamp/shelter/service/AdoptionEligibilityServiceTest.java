package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.AdoptionResult;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import lv.bootcamp.shelter.model.Adopter;
import java.util.Optional;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.audit.RejectionReason;
import lv.bootcamp.shelter.model.AnimalType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdoptionEligibilityServiceTest {

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService adoptionEligibilityService;

    @Test
    void evaluateAdoption_shouldRejectWhenAdopterNotFound() {
        when(adopterRepository.findById(1L))
                .thenReturn(Optional.empty());

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.ADOPTER_NOT_FOUND);
        assertThat(result.priorityScore()).isZero();

        verifyNoInteractions(
                animalRepository,
                notificationClient,
                auditLogger
        );
    }
    @Test
    void evaluateAdoption_shouldRejectWhenAnimalNotFound() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setName("Anna");

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.empty());

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.ANIMAL_NOT_FOUND);
        assertThat(result.priorityScore()).isZero();

        verifyNoInteractions(notificationClient, auditLogger);
    }
    @Test
    void evaluateAdoption_shouldRejectWhenAnimalNotAvailable() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setStatus(AnimalStatus.ADOPTED);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.ANIMAL_NOT_AVAILABLE);
        assertThat(result.priorityScore()).isZero();

        verifyNoInteractions(notificationClient, auditLogger);
    }
    @Test
    void evaluateAdoption_shouldRejectUnderageAdopter() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(17);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setStatus(AnimalStatus.AVAILABLE);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.UNDERAGE);

        verify(auditLogger).logRejection(
                1L, 2L, RejectionReason.UNDERAGE);

        verifyNoInteractions(notificationClient);
    }
    @Test
    void evaluateAdoption_shouldRejectWhenPetLimitIs3() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setCurrentPetCount(3);
        adopter.setLargeProperty(false);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setStatus(AnimalStatus.AVAILABLE);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.PET_LIMIT_REACHED);

        verify(auditLogger).logRejection(
                1L, 2L, RejectionReason.PET_LIMIT_REACHED);

        verifyNoInteractions(notificationClient);
    }
    @Test
    void evaluateAdoption_shouldRejectWhenLargePropertyPetLimitIs5() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setCurrentPetCount(5);
        adopter.setLargeProperty(true);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setStatus(AnimalStatus.AVAILABLE);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.PET_LIMIT_REACHED);

        verify(auditLogger).logRejection(
                1L, 2L, RejectionReason.PET_LIMIT_REACHED);

        verifyNoInteractions(notificationClient);
    }
    @Test
    void evaluateAdoption_shouldApprove18YearOldAdopter() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(18);
        adopter.setEmail("anna@example.com");

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(3);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isZero();

        verify(auditLogger).logApproval(1L, 2L, 0);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Rex");
    }
    @Test
    void evaluateAdoption_shouldRejectWhenExoticPermitMissing() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setExoticPermit(false);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Tweety");
        animal.setType(AnimalType.BIRD);
        animal.setStatus(AnimalStatus.AVAILABLE);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);
        assertThat(result.priorityScore()).isZero();

        verify(auditLogger).logRejection(
                1L, 2L, RejectionReason.NO_EXOTIC_PERMIT
        );

        verifyNoInteractions(notificationClient);
    }
    @Test
    void evaluateAdoption_shouldApproveNormalPropertyWith2Pets() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setCurrentPetCount(2);
        adopter.setLargeProperty(false);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(3);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isZero();

        verify(auditLogger).logApproval(1L, 2L, 0);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Rex");
    }

    @Test
    void evaluateAdoption_shouldApproveLargePropertyWith4Pets() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setCurrentPetCount(4);
        adopter.setLargeProperty(true);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(3);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isEqualTo(7);

        verify(auditLogger).logApproval(1L, 2L, 7);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Rex");
    }

    @Test
    void evaluateAdoption_shouldRejectRabbitWithoutPermit() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setExoticPermit(false);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Bunny");
        animal.setType(AnimalType.RABBIT);
        animal.setStatus(AnimalStatus.AVAILABLE);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason())
                .isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);
        assertThat(result.priorityScore()).isZero();

        verify(auditLogger).logRejection(
                1L, 2L, RejectionReason.NO_EXOTIC_PERMIT);

        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldApproveBirdWithPermit() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setExoticPermit(true);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Tweety");
        animal.setType(AnimalType.BIRD);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(2);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isZero();

        verify(auditLogger).logApproval(1L, 2L, 0);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Tweety");
    }

    @Test
    void evaluateAdoption_shouldApproveRabbitWithPermit() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setExoticPermit(true);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Bunny");
        animal.setType(AnimalType.RABBIT);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(2);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isZero();

        verify(auditLogger).logApproval(1L, 2L, 0);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Bunny");
    }

    @Test
    void evaluateAdoption_shouldCalculateScoreForOnePreviousAdoption() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setPreviousAdoptions(1);
        adopter.setCurrentPetCount(0);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(7);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isEqualTo(10);

        verify(auditLogger).logApproval(1L, 2L, 10);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Rex");
    }

    @Test
    void evaluateAdoption_shouldCalculateScoreForThreePreviousAdoptions() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setPreviousAdoptions(3);
        adopter.setCurrentPetCount(0);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(8);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isEqualTo(30);

        verify(auditLogger).logApproval(1L, 2L, 30);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Rex");
    }

    @Test
    void evaluateAdoption_shouldCalculateCombinedPriorityScore() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setPreviousAdoptions(4);
        adopter.setCurrentPetCount(2);
        adopter.setLargeProperty(true);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(8);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isEqualTo(46);

        verify(auditLogger).logApproval(1L, 2L, 46);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Rex");
    }

    @Test
    void evaluateAdoption_shouldNeverReturnNegativePriorityScore() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setAge(25);
        adopter.setEmail("anna@example.com");
        adopter.setPreviousAdoptions(0);
        adopter.setCurrentPetCount(1);
        adopter.setLargeProperty(false);

        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(null);

        when(adopterRepository.findById(1L))
                .thenReturn(Optional.of(adopter));

        when(animalRepository.findById(2L))
                .thenReturn(Optional.of(animal));

        AdoptionResult result =
                adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.priorityScore()).isZero();

        verify(auditLogger).logApproval(1L, 2L, 0);

        verify(notificationClient)
                .sendApprovalNotification(
                        "anna@example.com", "Rex");
    }
}