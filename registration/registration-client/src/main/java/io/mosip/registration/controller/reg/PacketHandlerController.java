package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.LoggerConstants.PACKET_HANDLER;
import static io.mosip.registration.constants.RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE_CODE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import io.mosip.registration.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationApprovalDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SyncDataProcessDTO;
import io.mosip.registration.entity.PreRegistrationList;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.config.JobConfigurationService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.packet.PacketHandlerService;
import io.mosip.registration.service.packet.ReRegistrationService;
import io.mosip.registration.service.packet.RegistrationApprovalService;
import io.mosip.registration.service.sync.PolicySyncService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.service.template.TemplateService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.acktemplate.TemplateGenerator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Class for Registration Packet operations
 * 
 * @author Sravya Surampalli
 * @since 1.0.0
 *
 */
@Controller
public class PacketHandlerController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(PacketHandlerController.class);

	@FXML
	private Button uinUpdateBtn;

	@FXML
	private ImageView uinUpdateImage;

	@FXML
	private ImageView newRegImage;

	@FXML
	private ImageView lostUINImage;

	@FXML
	private Button newRegistrationBtn;

	@FXML
	private GridPane uploadRoot;

	@FXML
	private Label pendingApprovalCountLbl;

	@FXML
	private Label reRegistrationCountLbl;

	@FXML
	private Label lastBiometricTime;

	@FXML
	private Label lastPreRegPacketDownloadedTime;

	@FXML
	private Label lastSyncTime;

	@Autowired
	private JobConfigurationService jobConfigurationService;

	@SuppressWarnings("unchecked")
	public void setLastUpdateTime() {
		try {

			ResponseDTO responseDTO = jobConfigurationService.getLastCompletedSyncJobs();
			if (responseDTO.getSuccessResponseDTO() != null) {

				List<SyncDataProcessDTO> dataProcessDTOs = ((List<SyncDataProcessDTO>) responseDTO
						.getSuccessResponseDTO().getOtherAttributes().get(RegistrationConstants.SYNC_DATA_DTO));

				LinkedList<String> timestamps = new LinkedList<>();
				dataProcessDTOs.forEach(syncDataProcessDTO -> {

					if (!(jobConfigurationService.getUnTaggedJobs().contains(syncDataProcessDTO.getJobId())
							|| jobConfigurationService.getOfflineJobs().contains(syncDataProcessDTO.getJobId()))) {
						timestamps.add(syncDataProcessDTO.getLastUpdatedTimes());
					}
				});

				String latestUpdateTime = timestamps.stream().sorted((timestamp1, timestamp2) -> Timestamp
						.valueOf(timestamp2).compareTo(Timestamp.valueOf(timestamp1))).findFirst().get();

				lastSyncTime.setText(getLocalZoneTime(latestUpdateTime));

				setLastPreRegPacketDownloadedTime();
			}
		} catch (RuntimeException exception) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
			lastSyncTime.setText("---");
		}
	}

	@FXML
	private GridPane eodProcessGridPane;

	@FXML
	private GridPane lostUINPane;

	@FXML
	private VBox vHolder;

	@FXML
	public GridPane uinUpdateGridPane;

	@FXML
	public GridPane newRegGridPane;

	@FXML
	public HBox userOnboardMessage;

	@Autowired
	private AckReceiptController ackReceiptController;

	@Autowired
	private TemplateService templateService;

	@Autowired
	private TemplateManagerBuilder templateManagerBuilder;

	@Autowired
	private TemplateGenerator templateGenerator;

	@Autowired
	PreRegistrationDataSyncService preRegistrationDataSyncService;

	@Autowired
	private UserOnboardController userOnboardController;

	@Autowired
	private PacketHandlerService packetHandlerService;

	@Autowired
	private DashBoardController dashBoardController;

	@Autowired
	private RegistrationApprovalService registrationApprovalService;

	@Autowired
	private ReRegistrationService reRegistrationService;

	@Autowired
	private UserOnboardParentController userOnboardParentController;
	
	@Autowired
	private PolicySyncService policySyncService;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private UserOnboardService userOnboardService;

	@FXML
	ProgressIndicator progressIndicator;

	@FXML
	public GridPane progressPane;

	@FXML
	public ProgressBar syncProgressBar;

	@FXML
	private Label eodLabel;

	@FXML
	private GridPane syncDataPane;
	@FXML
	private ImageView syncDataImageView;
	@FXML
	private GridPane downloadPreRegDataPane;
	@FXML
	private ImageView downloadPreRegDataImageView;
	@FXML
	private GridPane updateOperatorBiometricsPane;
	@FXML
	private ImageView updateOperatorBiometricsImageView;
	@FXML
	private GridPane eodApprovalPane;
	@FXML
	private ImageView eodApprovalImageView;
	@FXML
	private GridPane reRegistrationPane;
	@FXML
	private ImageView reRegistrationImageView;
	@FXML
	private GridPane dashBoardPane;
	@FXML
	private GridPane uploadPacketPane;
	@FXML
	private GridPane centerRemapPane;
	@FXML
	private GridPane checkUpdatesPane;
	@FXML
	private ImageView viewReportsImageView;
	@Autowired
	private SoftwareUpdateHandler softwareUpdateHandler;
	@FXML
	private Label versionValueLabel;

	@Autowired
	HeaderController headerController;

	@FXML
	private ImageView uploadPacketImageView;

	@FXML
	private ImageView remapImageView;

	@FXML
	private ImageView checkUpdatesImageView;
	
	@FXML
	private ImageView tickMarkImageView;

	@Value("${object.store.base.location}")
	private String baseLocation;

	@Value("${packet.manager.account.name}")
	private String packetsLocation;

	@Autowired
	private GenericController genericController;

	@Autowired
	private Validations validation;
	
	@Autowired
	private LanguageSelectionController languageSelectionController;

	/**
	 * @return the userOnboardMsg
	 */
	public HBox getUserOnboardMessage() {
		return userOnboardMessage;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		versionValueLabel.setText(softwareUpdateHandler.getCurrentVersion());

		try {
			setImagesOnHover();

			setImage(newRegImage, RegistrationConstants.NEW_REG_IMG);
			setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);	
			setImage(downloadPreRegDataImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);		
			setImage(remapImageView, RegistrationConstants.SYNC_IMG);
			setImage(checkUpdatesImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);		
			setImage(newRegImage, RegistrationConstants.NEW_REG_IMG);
			setImage(uinUpdateImage, RegistrationConstants.UIN_UPDATE_IMG);
			setImage(lostUINImage, RegistrationConstants.LOST_UIN_IMG);		
			setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_IMG);
			setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_IMG);		
			setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_IMG);
			setImage(tickMarkImageView, RegistrationConstants.TICK_IMG);
			setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);

			if (!Role.hasSupervisorRole(SessionContext.userContext().getRoles())) {
				eodProcessGridPane.setVisible(false);
				eodLabel.setVisible(false);
			}
			setLastUpdateTime();
			pendingApprovalCountLbl.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_PENDING_APPLICATIONS));
			reRegistrationCountLbl.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_RE_REGISTER_APPLICATIONS));

			List<RegistrationApprovalDTO> pendingApprovalRegistrations = registrationApprovalService
					.getEnrollmentByStatus(RegistrationClientStatusCode.CREATED.getCode());

			List<PacketStatusDTO> reRegisterRegistrations = reRegistrationService.getAllReRegistrationPackets();
			List<String> configuredFieldsfromDB = Arrays.asList(
					getValueFromApplicationContext(RegistrationConstants.UIN_UPDATE_CONFIG_FIELDS_FROM_DB).split(","));

			if (!pendingApprovalRegistrations.isEmpty()) {
				pendingApprovalCountLbl
						.setText(pendingApprovalRegistrations.size() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.APPLICATIONS));
			}
			if (!reRegisterRegistrations.isEmpty()) {
				reRegistrationCountLbl
						.setText(reRegisterRegistrations.size() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.APPLICATIONS));
			}
			if (!(getValueFromApplicationContext(RegistrationConstants.UIN_UPDATE_CONFIG_FLAG))
					.equalsIgnoreCase(RegistrationConstants.ENABLE)
					|| configuredFieldsfromDB.get(RegistrationConstants.PARAM_ZERO).isEmpty()) {
				vHolder.getChildren().forEach(btnNode -> {
					if (btnNode instanceof GridPane && btnNode.getId() != null
							&& btnNode.getId().equals(uinUpdateGridPane.getId())) {
						btnNode.setVisible(false);
						btnNode.setManaged(false);
					}
				});
			}
			Timestamp ts = userOnboardService.getLastUpdatedTime(SessionContext.userId());
			if (ts != null) {
				lastBiometricTime
						.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LAST_DOWNLOADED) + " " + getLocalZoneTime(ts.toString()));
			}

			if (!(getValueFromApplicationContext(RegistrationConstants.LOST_UIN_CONFIG_FLAG))
					.equalsIgnoreCase(RegistrationConstants.ENABLE)) {
				lostUINPane.setVisible(false);
			}
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("REGISTRATION - UI- Home Page Loading", APPLICATION_NAME, APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}

	private void setImagesOnHover() {

		newRegGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(newRegImage, RegistrationConstants.NEW_REGISTRATION_IMG);
				newRegImage.setImage(new Image(getClass().getResourceAsStream(RegistrationConstants.NEW_REG_FOCUSED)));
			} else {
				setImage(newRegImage, RegistrationConstants.NEW_REG_IMG);
			}
		});
		uinUpdateGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				
				setImage(uinUpdateImage, RegistrationConstants.UPDATE_UIN_FOCUSED_IMG);
			} else {
				setImage(uinUpdateImage, RegistrationConstants.UIN_UPDATE_IMG);
			}
		});
		lostUINPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(lostUINImage, RegistrationConstants.LOST_UIN_FOCUSED_IMG);
				lostUINImage
						.setImage(new Image(getClass().getResourceAsStream(RegistrationConstants.LOST_UIN_FOCUSED)));
			} else {

				setImage(lostUINImage, RegistrationConstants.LOST_UIN_IMG);
			}
		});
		syncDataPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				
				setImage(syncDataImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {
				
				setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		downloadPreRegDataPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(downloadPreRegDataImageView, RegistrationConstants.DOWNLOAD_PREREG_FOCUSED_IMG);
			} else {

				setImage(downloadPreRegDataImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			}
		});
		updateOperatorBiometricsPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OP_BIOMETRICS_FOCUSED_IMG);
			} else {
				setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);
			}
		});
		eodApprovalPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_FOCUSED_IMG);
			} else {
				setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_IMG);
			}
		});
		reRegistrationPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_FOCUSED_IMG);	
			} else {
				setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_IMG);	
			}
		});
		dashBoardPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_FOCUSED_IMG);
			} else {
				setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_IMG);
			}
		});
		uploadPacketPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OP_BIOMETRICS_FOCUSED_IMG);
			} else {

				setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);
			}
		});
		centerRemapPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(remapImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {

				setImage(remapImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		checkUpdatesPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(checkUpdatesImageView, RegistrationConstants.DOWNLOAD_PREREG_FOCUSED_IMG);
			} else {
				setImage(checkUpdatesImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			}
		});
	}

	/**
	 * Validating screen authorization and Creating Packet and displaying
	 * acknowledgement form
	 */
	public void createPacket() {
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Creation of Registration Starting.");
		try {
			auditFactory.audit(AuditEvent.NAV_NEW_REG, Components.NAVIGATION, SessionContext.userContext().getUserId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			Parent createRoot = getRoot(RegistrationConstants.CREATE_PACKET_PAGE);
			LOGGER.info("REGISTRATION - CREATE_PACKET - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
					APPLICATION_ID, "Validating Create Packet screen for specific role");

			if (!validateScreenAuthorization(createRoot.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			getScene(createRoot).setRoot(createRoot);
			getScene(createRoot).getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
			validation.updateAsLostUIN(false);

			if(registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_NEW)) {
				genericController.populateScreens();
				return;
			}
		} catch (Exception exception) {
			LOGGER.error("Failed to start registration", exception);
		}
		clearRegistrationData();
		generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
	}

	/**
	 * Validating screen authorization and Creating Packet in case of Lost UIN
	 */
	public void lostUIN() {
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
				"Creating of Registration for lost UIN Starting.");
		try {
			auditFactory.audit(AuditEvent.NAV_LOST_UIN, Components.NAVIGATION, SessionContext.userContext().getUserId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			Parent createRoot = getRoot(RegistrationConstants.CREATE_PACKET_PAGE);
			LOGGER.info("REGISTRATION - CREATE_PACKET - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
					APPLICATION_ID, "Validating Create Packet screen for specific role");

			if (!validateScreenAuthorization(createRoot.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			getScene(createRoot).setRoot(createRoot);
			validation.updateAsLostUIN(true);
			if(registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_LOST)) {
				genericController.populateScreens();
				return;
			}
		} catch (Exception exception) {
			LOGGER.error("Failed to start Lost UIN", exception);
		}
		clearRegistrationData();
		generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
	}

	public void showReciept() {
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Showing receipt Started.");
		try {
			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();

			String platformLanguageCode = ApplicationContext.applicationLanguage();
			String ackTemplateText = templateService.getHtmlTemplate(ACKNOWLEDGEMENT_TEMPLATE_CODE,
					platformLanguageCode);

			if (ackTemplateText != null && !ackTemplateText.isEmpty()) {
				String key = "mosip.registration.important_guidelines_" + applicationContext.getApplicationLanguage();
				String guidelines = getValueFromApplicationContext(key);
				templateGenerator.setGuidelines(guidelines);
				ResponseDTO templateResponse = templateGenerator.generateTemplate(ackTemplateText, registrationDTO,
						templateManagerBuilder, RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE);
				if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
					Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
							.get(RegistrationConstants.TEMPLATE_NAME);
					ackReceiptController.setStringWriter(stringWriter);
					ResponseDTO packetCreationResponse = savePacket(stringWriter, registrationDTO);
					if (packetCreationResponse.getSuccessResponseDTO() != null) {
						Parent createRoot = getRoot(RegistrationConstants.ACK_RECEIPT_PATH);
						getScene(createRoot).setRoot(createRoot);
						setIsAckOpened(true);
						return;
					} else {
						clearRegistrationData();
						createPacket();
					}
				} else if (templateResponse != null && templateResponse.getErrorResponseDTOs() != null) {
					generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_ACKNOWLEDGEMENT_PAGE));
					clearRegistrationData();
					createPacket();
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_ACKNOWLEDGEMENT_PAGE));
				clearRegistrationData();
				createPacket();
			}
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - UI- Officer Packet Create ", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("REGISTRATION - UI- Officer Packet Create ", APPLICATION_NAME, APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Showing receipt ended.");
	}

	/**
	 * Validating screen authorization and Approve, Reject and Hold packets
	 */
	public void approvePacket() {

		LOGGER.info("Loading Pending Approval screen started.");
		try {
			auditFactory.audit(AuditEvent.NAV_APPROVE_REG, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			GridPane root = BaseController.load(getClass().getResource(RegistrationConstants.PENDING_APPROVAL_PAGE));

			LOGGER.info("Validating Approve Packet screen for specific role");

			if (Role.isDefaultUser(SessionContext.userContext().getRoles())) {
				getScene(root);
			} else if (!validateScreenAuthorization(root.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
			} else {
				getScene(root);
			}
		} catch (IOException ioException) {
			LOGGER.error(ioException.getMessage(), ioException);

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE));
		}
		LOGGER.info("Loading Pending Approval screen ended.");
	}

	/**
	 * Validating screen authorization and Uploading packets to FTP server
	 */
	public void uploadPacket() {

		if (!proceedOnAction("PS")) {
			return;
		}

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Packet Upload screen started.");
		try {
			auditFactory.audit(AuditEvent.NAV_UPLOAD_PACKETS, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			uploadRoot = BaseController.load(getClass().getResource(RegistrationConstants.FTP_UPLOAD_PAGE));

			LOGGER.info("REGISTRATION - UPLOAD_PACKET - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
					APPLICATION_ID, "Validating Upload Packet screen for specific role");

			if (!validateScreenAuthorization(uploadRoot.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
			} else {
				getScene(uploadRoot);

				// Clear all registration data
				clearRegistrationData();

				// Enable Auto-Logout
				SessionContext.setAutoLogout(true);

			}
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - UI- Officer Packet upload", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Packet Upload screen ended.");
	}

	public void updateUIN() {
		if (!proceedOnRegistrationAction())
			return;

		ResponseDTO keyResponse = isKeyValid();
		if (null == keyResponse.getSuccessResponseDTO()) {
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.INVALID_KEY));
			return;
		}

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Update UIN screen started.");
		try {
			auditFactory.audit(AuditEvent.NAV_UIN_UPDATE, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			if (RegistrationConstants.DISABLE
					.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG))
					&& RegistrationConstants.DISABLE.equalsIgnoreCase(
							getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))) {

				generateAlert(RegistrationConstants.ERROR,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPDATE_UIN_NO_BIOMETRIC_CONFIG_ALERT));
			} else {
				Parent root = BaseController.load(getClass().getResource(RegistrationConstants.UIN_UPDATE), 
						applicationContext.getBundle(registrationController.getSelectedLangList().get(0), RegistrationConstants.LABELS));

				LOGGER.info("REGISTRATION - update UIN - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
						APPLICATION_ID, "updating UIN");

				if (!validateScreenAuthorization(root.getId())) {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				} else {

					StringBuilder errorMessage = new StringBuilder();
					ResponseDTO responseDTO;
					responseDTO = validateSyncStatus();
					List<ErrorResponseDTO> errorResponseDTOs = responseDTO.getErrorResponseDTOs();
					if (errorResponseDTOs != null && !errorResponseDTOs.isEmpty()) {
						for (ErrorResponseDTO errorResponseDTO : errorResponseDTOs) {
							errorMessage.append(
									RegistrationUIConstants.getMessageLanguageSpecific(errorResponseDTO.getMessage())
											+ "\n\n");
						}
						generateAlert(RegistrationConstants.ERROR, errorMessage.toString().trim());

					} else {
						getScene(root);
					}
				}
			}
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - UI- UIN Update", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(ioException));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Update UIN screen ended.");
	}

	/**
	 * Sync data through batch jobs.
	 */
	public void syncData() {
		headerController.syncData(null);
	}

	/**
	 * This method is to trigger the Pre registration sync service
	 */
	@FXML
	public void downloadPreRegData() {

		headerController.downloadPreRegData(null);
	}

	/**
	 * change On-Board user Perspective
	 */
	public void onBoardUser() {
		if (!proceedOnAction("OU"))
			return;

		auditFactory.audit(AuditEvent.NAV_ON_BOARD_USER, Components.NAVIGATION, APPLICATION_NAME,
				AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

		SessionContext.map().put(RegistrationConstants.ONBOARD_USER, true);
		SessionContext.map().put(RegistrationConstants.ONBOARD_USER_UPDATE, true);

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading User Onboard Update page");

		try {
			GridPane headerRoot = BaseController.load(getClass().getResource(RegistrationConstants.USER_ONBOARD));
			getScene(headerRoot);
			userOnboardParentController.userOnboardId.lookup("#onboardUser").setVisible(false);
		} catch (IOException ioException) {
			LOGGER.error("Failed to load user onboard page", ioException);
		}
		userOnboardController.initUserOnboard();
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "User Onboard Update page is loaded");
	}

	/**
	 * To save the acknowledgement receipt along with the registration data and
	 * create packet
	 */
	private ResponseDTO savePacket(Writer stringWriter, RegistrationDTO registrationDTO) {
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "packet creation has been started");
		byte[] ackInBytes = null;
		try {
			ackInBytes = stringWriter.toString().getBytes(RegistrationConstants.TEMPLATE_ENCODING);
		} catch (java.io.IOException ioException) {
			LOGGER.error("REGISTRATION - SAVE_PACKET - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
					APPLICATION_ID, ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}

		/*if (RegistrationConstants.ENABLE
				.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.ACK_INSIDE_PACKET))) {
			registrationDTO.setAcknowledgeReceipt(ackInBytes);
			registrationDTO.setAcknowledgeReceiptName(
					"RegistrationAcknowledgement." + RegistrationConstants.ACKNOWLEDGEMENT_FORMAT);
		}*/

		// packet creation
		ResponseDTO response = packetHandlerService.handle(registrationDTO);

		if (response.getSuccessResponseDTO() != null
				&& response.getSuccessResponseDTO().getMessage().equals(RegistrationConstants.SUCCESS)) {

			try {
				// Deletes the pre registration Data after creation of registration Packet.
				if (getRegistrationDTOFromSession().getPreRegistrationId() != null
						&& !getRegistrationDTOFromSession().getPreRegistrationId().trim().isEmpty()) {

					ResponseDTO responseDTO = new ResponseDTO();
					List<PreRegistrationList> preRegistrationLists = new ArrayList<>();
					PreRegistrationList preRegistrationList = preRegistrationDataSyncService
							.getPreRegistrationRecordForDeletion(
									getRegistrationDTOFromSession().getPreRegistrationId());
					preRegistrationLists.add(preRegistrationList);
					preRegistrationDataSyncService.deletePreRegRecords(responseDTO, preRegistrationLists);

				}

				// Generate the file path for storing the Encrypted Packet and Acknowledgement
				// Receipt
				String separator = "/";
				String filePath = baseLocation.concat(separator).concat(packetsLocation).concat(separator)
						.concat(registrationDTO.getRegistrationId());

				// Storing the Registration Acknowledge Receipt Image
				FileUtils.copyToFile(new ByteArrayInputStream(ackInBytes),
						new File(filePath.concat("_Ack.").concat(RegistrationConstants.ACKNOWLEDGEMENT_FORMAT)));

				// TODO - Client should not send notification, save contact details
				// TODO - so that it can be sent out during RID sync.
//				sendNotification((String) registrationDTO.getDemographics().get("email"),
//						(String) registrationDTO.getDemographics().get("phone"), registrationDTO.getRegistrationId());

				// Sync and Uploads Packet when EOD Process Configuration is set to OFF
				String supervisorApproval = getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_APPROVAL_CONFIG_FLAG);
				if (supervisorApproval != null && !getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_APPROVAL_CONFIG_FLAG)
						.equalsIgnoreCase(RegistrationConstants.ENABLE)) {
					updatePacketStatus();//auto-approve
				}

				LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
						"Registration's Acknowledgement Receipt saved");
			} catch (io.mosip.kernel.core.exception.IOException ioException) {
				LOGGER.error("REGISTRATION - SAVE_PACKET - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
						APPLICATION_ID, ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			} catch (RegBaseCheckedException regBaseCheckedException) {
				LOGGER.error("REGISTRATION - SAVE_PACKET - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
						APPLICATION_ID,
						regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));

				if (regBaseCheckedException.getErrorCode()
						.equals(RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode())) {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTH_ADVICE_FAILURE));
				}
			} catch (RuntimeException runtimeException) {
				LOGGER.error("REGISTRATION - SAVE_PACKET - REGISTRATION_OFFICER_PACKET_CONTROLLER", APPLICATION_NAME,
						APPLICATION_ID, runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			}
		} else {
			if (response.getErrorResponseDTOs() != null && response.getErrorResponseDTOs().get(0).getCode()
					.equals(RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTH_ADVICE_FAILURE));
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_CREATION_FAILURE));
			}
		}
		return response;
	}

	/**
	 * Load re registration screen.
	 */
	public void loadReRegistrationScreen() {
		if (!proceedOnReRegistrationAction()) {
			return;
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading re-registration screen sarted.");

		try {
			auditFactory.audit(AuditEvent.NAV_RE_REGISTRATION, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			Parent root = BaseController.load(getClass().getResource(RegistrationConstants.REREGISTRATION_PAGE));

			LOGGER.info("REGISTRATION - LOAD_REREGISTRATION_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID, "Loading reregistration screen");

			getScene(root);
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - LOAD_REREGISTRATION_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading re-registration screen ended.");
	}

	public void viewDashBoard() {

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen sarted.");

		try {
			auditFactory.audit(AuditEvent.NAV_DASHBOARD, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			String dashboardTemplateText = templateService.getHtmlTemplate(
					RegistrationConstants.DASHBOARD_TEMPLATE_CODE, ApplicationContext.applicationLanguage());

			ResponseDTO templateResponse = templateGenerator.generateDashboardTemplate(dashboardTemplateText,
					templateManagerBuilder, RegistrationConstants.DASHBOARD_TEMPLATE,
					Initialization.getApplicationStartTime());

			if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
				Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
						.get(RegistrationConstants.DASHBOARD_TEMPLATE);
				dashBoardController.setStringWriter(stringWriter);
				Parent root = BaseController.load(getClass().getResource(RegistrationConstants.DASHBOARD_PAGE));

				LOGGER.info("REGISTRATION - LOAD_DASHBOARD_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
						APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen");

				getScene(root);
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_DASHBOARD_PAGE));
			}
		} catch (IOException | RegBaseCheckedException exception) {
			LOGGER.error("REGISTRATION - LOAD_DASHBOARD_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID, exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_DASHBOARD_PAGE));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen ended.");
	}

	/**
	 * Update packet status.
	 * 
	 * @throws RegBaseCheckedException
	 */
	private void updatePacketStatus() throws RegBaseCheckedException {
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
				"Auto Approval of Packet when EOD process disabled started");

		registrationApprovalService.updateRegistration((getRegistrationDTOFromSession().getRegistrationId()),
				RegistrationConstants.EMPTY, RegistrationClientStatusCode.APPROVED.getCode());

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
				"Auto Approval of Packet when EOD process disabled ended");

	}

	private ResponseDTO isKeyValid() {

		return policySyncService.checkKeyValidation();

	}

	public ProgressIndicator getProgressIndicator() {
		return progressIndicator;
	}

	public void setLastPreRegPacketDownloadedTime() {

		SyncControl syncControl = jobConfigurationService
				.getSyncControlOfJob(RegistrationConstants.OPT_TO_REG_PDS_J00003);

		if (syncControl != null) {
			Timestamp lastPreRegPacketDownloaded = syncControl.getLastSyncDtimes();

			if (lastPreRegPacketDownloaded != null) {
				lastPreRegPacketDownloadedTime.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LAST_DOWNLOADED) + " "
						+ getLocalZoneTime(lastPreRegPacketDownloaded.toString()));
			}
		}
	}

	@FXML
	public void uploadPacketToServer() {
		auditFactory.audit(AuditEvent.SYNC_PRE_REGISTRATION_PACKET, Components.SYNC_SERVER_TO_CLIENT,
				SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		uploadPacket();
	}

	@FXML
	public void intiateRemapProcess() {
		headerController.intiateRemapProcess();
	}

	@FXML
	public void hasUpdate() {
		headerController.hasUpdate(null);
	}
	
	public void selectLanguage(MouseEvent event) {
		if (!proceedOnRegistrationAction())
			return;

		ResponseDTO keyResponse = isKeyValid();
		if (null == keyResponse.getSuccessResponseDTO()) {
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.INVALID_KEY));
			return;
		}
		StringBuilder errorMessage = new StringBuilder();
		ResponseDTO responseDTO;
		responseDTO = validateSyncStatus();
		List<ErrorResponseDTO> errorResponseDTOs = responseDTO.getErrorResponseDTOs();
		if (errorResponseDTOs != null && !errorResponseDTOs.isEmpty()) {
			for (ErrorResponseDTO errorResponseDTO : errorResponseDTOs) {
				errorMessage.append(
						RegistrationUIConstants.getMessageLanguageSpecific(errorResponseDTO.getMessage())
								+ "\n\n");
			}
			generateAlert(RegistrationConstants.ERROR, errorMessage.toString().trim());
		} else {
			String action = RegistrationConstants.EMPTY;
			if (((GridPane) event.getSource()).equals(newRegGridPane)) {
				action = RegistrationConstants.NEW_REGISTRATION_FLOW;
			} else if (((GridPane) event.getSource()).equals(uinUpdateGridPane)) {
				action = RegistrationConstants.UIN_UPDATE_FLOW;
			} else if (((GridPane) event.getSource()).equals(lostUINPane)) {
				action = RegistrationConstants.LOST_UIN_FLOW;
			}

			try {
				if(isLanguageSelectionRequired()) {
					getStage().getScene().getRoot().setDisable(true);
					languageSelectionController.init(action);
				}
				else {
					languageSelectionController.submitLanguagesAndProceed(action, baseService.getMandatoryLanguages());
				}
			} catch (PreConditionCheckException e) {
				generateAlert(RegistrationConstants.ERROR, e.getErrorCode());
			}
		}
	}

	private boolean isLanguageSelectionRequired() throws PreConditionCheckException {
		return ( baseService.getMinLanguagesCount() >= 1 && baseService.getMaxLanguagesCount() > 1 );
	}
}
