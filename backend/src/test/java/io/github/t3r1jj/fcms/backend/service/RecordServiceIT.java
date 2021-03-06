package io.github.t3r1jj.fcms.backend.service;

import io.github.t3r1jj.fcms.backend.model.StoredRecord;
import io.github.t3r1jj.fcms.backend.model.StoredRecordMeta;
import io.github.t3r1jj.fcms.backend.repository.StoredRecordMetaRepository;
import io.github.t3r1jj.fcms.backend.repository.StoredRecordRepository;
import io.github.t3r1jj.fcms.external.data.RecordMeta;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@SpringBootTest
public class RecordServiceIT extends AbstractTestNGSpringContextTests {

    private RecordService recordService;
    @Autowired
    private StoredRecordRepository recordRepository;
    @Autowired
    private StoredRecordMetaRepository metaRepository;
    @Mock
    private ReplicationService replicationService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        recordService = new RecordService(recordRepository, metaRepository, replicationService);
    }

    @AfterMethod
    public void tearDown() {
        recordRepository.deleteAll();
    }

    @Test
    public void store() {
        StoredRecord storedRecord = new StoredRecord("a", "a");
        StoredRecord childRecord = new StoredRecord("2", "2", null, storedRecord.getId().toString());
        recordService.store(storedRecord);
        recordService.store(childRecord);
        assertEquals(recordRepository.count(), 1);
        storedRecord = recordService.findAll().iterator().next();
        assertEquals(storedRecord.getVersions().get(0), childRecord);
    }

    @Test
    public void updateRoot() {
        StoredRecord storedRecord = new StoredRecord("a", "a");
        recordService.store(storedRecord);
        storedRecord.getBackups().put("new backup", null);
        recordService.update(storedRecord);
        storedRecord = recordRepository.findById(storedRecord.getId()).get();
        assertEquals(recordRepository.count(), 1 );
        assertEquals(storedRecord.getBackups().size(), 1);
    }

    @Test
    public void updateChild() {
        StoredRecord storedRecord = new StoredRecord("a", "a");
        StoredRecord childRecord = new StoredRecord("2", "2", null, storedRecord.getId().toString());
        recordService.store(storedRecord);
        recordService.store(childRecord);
        childRecord.getBackups().put("new backup", new RecordMeta("x","y",0));
        recordService.update(childRecord);
        storedRecord = recordRepository.findById(storedRecord.getId()).get();
        assertEquals(recordRepository.count(), 1);
        assertEquals(storedRecord.getVersions().get(0).getBackups().size(), 1);
    }

    @Test
    public void findAll() {
        StoredRecord storedRecord = new StoredRecord("a", "a");
        StoredRecord storedRecord2 = new StoredRecord("2", "2");
        StoredRecord storedRecord3 = new StoredRecord("33", "33", null, storedRecord2.getId().toString());
        recordService.store(storedRecord);
        assertEquals(recordService.findAll().size(), 1);
        recordService.store(storedRecord2);
        assertEquals(recordService.findAll().size(), 2);
        recordService.store(storedRecord3);
        assertEquals(recordService.findAll().size(), 2);
    }

    @Test
    public void updateMeta() {
        StoredRecord storedRecord = new StoredRecord("a", "a");
        StoredRecordMeta meta = storedRecord.getMeta();
        recordService.store(storedRecord);
        StoredRecordMeta newMeta = new StoredRecordMeta(meta.getId(), "2", "3", "4");
        recordService.updateMeta(newMeta);
        assertEquals(recordRepository.findById(storedRecord.getId()).get().getMeta(), newMeta);
    }

    @Test
    public void storeShouldCauseReplication() {
        StoredRecord storedRecord = new StoredRecord("a", "a");
        recordService.store(storedRecord);
        verify(replicationService, times(1)).uploadToPrimary(storedRecord);
    }
}