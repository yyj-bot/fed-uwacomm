package com.feduwacomm.service;

import com.feduwacomm.entity.InitialModel;
import com.feduwacomm.enums.GenerationMethod;
import com.feduwacomm.enums.InitialModelBindingStatus;
import com.feduwacomm.enums.InitialModelStatus;
import com.feduwacomm.enums.ModelType;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.model.vo.initial.InitialModelDownloadVO;
import com.feduwacomm.service.impl.InitialModelGenerationServiceImpl;
import com.feduwacomm.service.sklearn.SklearnModelParameterGeneratorFactory;
import com.feduwacomm.service.sklearn.SklearnModelParameterValidator;
import com.feduwacomm.utils.UuidUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialModelGenerationServiceImplDownloadTest {

    private static final String MODEL_ID = "model-1";

    @InjectMocks
    private InitialModelGenerationServiceImpl initialModelGenerationService;

    @Mock
    private InitialModelMapper initialModelMapper;
    @Mock
    private ModelDistributionMapper modelDistributionMapper;
    @Mock
    private FederatedTasksMapper federatedTasksMapper;
    @Mock
    private TaskParticipantsMapper taskParticipantsMapper;
    @Mock
    private GlobalModelDistributionService globalModelDistributionService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock
    private UuidUtil uuidUtil;
    @Mock
    private SklearnModelParameterGeneratorFactory parameterGeneratorFactory;
    @Mock
    private SklearnModelParameterValidator parameterValidator;

    private Path tempFile;

    @AfterEach
    void tearDown() throws Exception {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
        tempFile = null;
    }

    @Test
    void shouldReturnFileResourceWhenStoragePathExists() throws Exception {
        tempFile = Files.createTempFile("initial-model", ".bin");
        Files.write(tempFile, "binary-content".getBytes(StandardCharsets.UTF_8));

        InitialModel model = InitialModel.builder()
            .id(MODEL_ID)
            .modelType(ModelType.RANDOM_FOREST)
            .generationMethod(GenerationMethod.AUTO)
            .storagePath(tempFile.toString())
            .status(InitialModelStatus.READY)
            .bindingStatus(InitialModelBindingStatus.UNBOUND)
            .build();

        when(initialModelMapper.selectById(MODEL_ID)).thenReturn(model);

        InitialModelDownloadVO download = initialModelGenerationService.prepareModelDownload(MODEL_ID);

        assertThat(download.isFileResource()).isTrue();
        assertThat(download.getFilePath()).isEqualTo(tempFile.toString());
        assertThat(download.getContentLength()).isEqualTo(Files.size(tempFile));
        assertThat(download.getInlineContent()).isNull();
    }

    @Test
    void shouldFallbackToInlineContentWhenFileMissingButDataExists() {
        InitialModel model = InitialModel.builder()
            .id(MODEL_ID)
            .modelType(ModelType.NEURAL_NETWORK)
            .generationMethod(GenerationMethod.AUTO)
            .storagePath("/path/not/exist/model.bin")
            .modelData("{\"weights\": [0.1, 0.2]}" )
            .status(InitialModelStatus.READY)
            .bindingStatus(InitialModelBindingStatus.UNBOUND)
            .build();

        when(initialModelMapper.selectById(MODEL_ID)).thenReturn(model);

        InitialModelDownloadVO download = initialModelGenerationService.prepareModelDownload(MODEL_ID);

        assertThat(download.isFileResource()).isFalse();
        assertThat(download.getFilename()).contains(MODEL_ID);
        assertThat(download.getContentType()).isEqualTo("application/json");
        assertThat(download.getInlineContent()).isNotEmpty();
        verify(initialModelMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void shouldFailWhenNoFileAndNoData() {
        InitialModel model = InitialModel.builder()
            .id(MODEL_ID)
            .modelType(ModelType.RANDOM_FOREST)
            .generationMethod(GenerationMethod.CUSTOM)
            .status(InitialModelStatus.READY)
            .bindingStatus(InitialModelBindingStatus.UNBOUND)
            .build();

        when(initialModelMapper.selectById(MODEL_ID)).thenReturn(model);
        when(initialModelMapper.updateStatus(eq(MODEL_ID), eq(InitialModelStatus.FAILED.getCode()), any()))
            .thenReturn(1);

        assertThatThrownBy(() -> initialModelGenerationService.prepareModelDownload(MODEL_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("无法下载");

        verify(initialModelMapper).updateStatus(eq(MODEL_ID), eq(InitialModelStatus.FAILED.getCode()), any());
    }
}
