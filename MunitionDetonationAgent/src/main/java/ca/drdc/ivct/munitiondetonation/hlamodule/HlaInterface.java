package ca.drdc.ivct.munitiondetonation.hlamodule;

import ca.drdc.ivct.coders.warfare.MunitionDetonationCoder;
import ca.drdc.ivct.fom.warfare.MunitionDetonation;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HlaInterface extends NullFederateAmbassador {
    private InteractionClassHandle munitionDetonationInteractionClassHandle;

    private ParameterHandle articulatedPartDataHandle;
    private ParameterHandle detonationLocParameterHandle;
    private ParameterHandle detonationResultParameterHandle;
    private ParameterHandle eventIdParameterHandle;
    private ParameterHandle firingObjIdParameterHandle;
    private ParameterHandle finalVelVectorParameterHandle;
    private ParameterHandle fuseTypeParameterHandle;
    private ParameterHandle munitionObjIdParameterHandle;
    private ParameterHandle munitionTypeParameterHandle;
    private ParameterHandle quantityFiredParameterHandle;
    private ParameterHandle rateOfFireParameterHandle;
    private ParameterHandle relativeDetonationParameterHandle;
    private ParameterHandle targetObjIdParameterHandle;
    private ParameterHandle warheadTypeParameterHandle;

    private static final String MUNITION_DETONATION = "MunitionDetonation";

    private static final String ARTICULATED_PART_DATA = "ArticulatedPartData";
    private static final String DETONATION_LOC = "DetonationLocation";
    private static final String DETONATION_RESULT = "DetonationResultCode";
    private static final String EVENT_ID = "EventIdentifier";
    private static final String FIRING_OBJ_ID = "FiringObjectIdentifier";
    private static final String FINAL_VELOCITY = "FinalVelocityVector";
    private static final String FUSE_TYPE = "FuseType";
    private static final String MUNIT_OBJ_ID = "MunitionObjectIdentifier";
    private static final String MUNIT_TYPE = "MunitionType";
    private static final String QUANT_FIRED = "QuantityFired";
    private static final String RATE_OF_FIRE = "RateOfFire";
    private static final String RELATIVE_DETONATION_LOC = "RelativeDetonationLocation";
    private static final String TARGET_OBJ_ID = "TargetObjectIdentifier";
    private static final String WARHEAD_TYPE = "WarheadType";

    private static Logger logger = LoggerFactory.getLogger(HlaInterface.class);

    private RTIambassador ambassador;
    private EncoderFactory encoderFactory;

    /**
     * Connect to a CRC and join federation
     *
     * @param localSettingsDesignator The name to load settings for or "" to load default settings
     * @param fomPath                 path to FOM file
     * @param federationName          Name of the federation to join
     * @param federateName            The name you want for your federate
     * @throws RestoreInProgress              the action cannot be done because the system is restoring state
     * @throws SaveInProgress                 the action cannot be done because the system is saving state
     * @throws NotConnected                   if the federate is not connected to a CRC
     * @throws RTIinternalError               if the RTI fail unexpectedly
     * @throws ConnectionFailed               if the RTI fail
     * @throws InvalidLocalSettingsDesignator InvalidLocalSettingsDesignator
     * @throws ErrorReadingFDD                if error with the FDD
     * @throws CouldNotOpenFDD                if error with the FDD
     * @throws InconsistentFDD                if error with the FDD
     */
    public void start(String localSettingsDesignator, String fomPath, String federationName, String federateName)
        throws RestoreInProgress, SaveInProgress, NotConnected,
        RTIinternalError, ConnectionFailed, InvalidLocalSettingsDesignator, ErrorReadingFDD, CouldNotOpenFDD,
        InconsistentFDD {

        RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
        ambassador = rtiFactory.getRtiAmbassador();

        this.encoderFactory = rtiFactory.getEncoderFactory();

        try {
            ambassador.connect(this, CallbackModel.HLA_IMMEDIATE, localSettingsDesignator);
        } catch (UnsupportedCallbackModel | CallNotAllowedFromWithinCallback e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        } catch (AlreadyConnected ignored) { }

        try {
            ambassador.destroyFederationExecution(federationName);
        } catch (FederatesCurrentlyJoined e) {
            System.out.println(this.toString() + ": Tried to destroy federation " + federationName
                + " but federation still has active federates.");
        } catch (FederationExecutionDoesNotExist ignored) { }

        URL[] url = loadFomModules(fomPath);
        try {
            ambassador.createFederationExecution(federationName, url);
        } catch (FederationExecutionAlreadyExists e) {
            System.out.println(this.toString() + ": Tried to create federation " + federationName
                + " but the federation already exists.");
        }

        try {
            boolean joined = false;
            String federateNameSuffix = "";
            int federateNameIndex = 1;
            while (!joined) {
                try {
                    ambassador.joinFederationExecution(federateName + federateNameSuffix,
                        "MDSimJ", federationName, url);
                    joined = true;
                } catch (FederateNameAlreadyInUse e) {
                    federateNameSuffix = "-" + federateNameIndex++;
                }
            }
        } catch (CouldNotCreateLogicalTimeFactory | FederationExecutionDoesNotExist | CallNotAllowedFromWithinCallback e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        } catch (FederateAlreadyExecutionMember ignored) { }

        try {
            getHandles();
            publishInteractions();
        } catch (FederateNotExecutionMember e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        }
    }

    /**
     * Resign and disconnect from CRC
     *
     * @throws RTIinternalError if an internal error happen when stopping.
     */
    public void stop() throws RTIinternalError {
        if (ambassador == null) {
            System.out.println("HLAHandler.stop: _ambassador doesn't exist!");
            return;
        }
        try {
            try {
                ambassador.resignFederationExecution(ResignAction.CANCEL_THEN_DELETE_THEN_DIVEST);
            } catch (FederateNotExecutionMember ignored) {
                System.out.println("HlaInterface.stop: FederateNotExecutionMember1 exception: "
                    + ignored);
            } catch (FederateOwnsAttributes e) {
                System.out.println("HlaInterface.stop: FederateOwnsAttributes exception: " + e);
                throw new RTIinternalError("HlaInterfaceFailure", e);
            } catch (OwnershipAcquisitionPending e) {
                System.out.println("HlaInterface.stop: OwnershipAcquisitionPending exception: " + e);
                throw new RTIinternalError("HlaInterfaceFailure", e);
            } catch (CallNotAllowedFromWithinCallback e) {
                System.out.println("HlaInterface.stop: CallNotAllowedFromWithinCallback1 exception: "
                    + e);
                throw new RTIinternalError("HlaInterfaceFailure", e);
            } catch (InvalidResignAction e) {
                System.out.println("HlaInterface.stop: InvalidResignAction exception: " + e);
                throw new RTIinternalError("HlaInterfaceFailure", e);
            }


            try {
                ambassador.disconnect();
            } catch (FederateIsExecutionMember e) {
                System.out.println("HlaInterface.stop: FederateIsExecutionMember exception: " + e);
                throw new RTIinternalError("HlaInterfaceFailure", e);

            } catch (CallNotAllowedFromWithinCallback e) {
                System.out.println("HlaInterface.stop: CallNotAllowedFromWithinCallback2 exception: "
                    + e);
                throw new RTIinternalError("HlaInterfaceFailure", e);
            }
        } catch (NotConnected ignored) {
            System.out.println("HlaInterface.stop: NotConnected exception: " + ignored);
        }
    }

    private void getHandles() throws RTIinternalError, FederateNotExecutionMember, NotConnected {
        try {
            munitionDetonationInteractionClassHandle = ambassador.getInteractionClassHandle(MUNITION_DETONATION);


            articulatedPartDataHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, ARTICULATED_PART_DATA);
            detonationLocParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, DETONATION_LOC);
            detonationResultParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, DETONATION_RESULT);
            eventIdParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, EVENT_ID);
            firingObjIdParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, FIRING_OBJ_ID);
            finalVelVectorParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, FINAL_VELOCITY);
            fuseTypeParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, FUSE_TYPE);
            munitionObjIdParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, MUNIT_OBJ_ID);
            munitionTypeParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, MUNIT_TYPE);
            quantityFiredParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, QUANT_FIRED);
            rateOfFireParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, RATE_OF_FIRE);
            relativeDetonationParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, RELATIVE_DETONATION_LOC);
            targetObjIdParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, TARGET_OBJ_ID);
            warheadTypeParameterHandle = ambassador.getParameterHandle(munitionDetonationInteractionClassHandle, WARHEAD_TYPE);

        } catch (NameNotFound | InvalidInteractionClassHandle e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        }
    }

    private void publishInteractions() throws FederateNotExecutionMember, RestoreInProgress,
        SaveInProgress, NotConnected, RTIinternalError {
        try {
            ambassador.publishInteractionClass(munitionDetonationInteractionClassHandle);
        } catch (InteractionClassNotDefined e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        }
    }

    @Override
    public void connectionLost(String faultDescription) throws FederateInternalError {
        logger.error("HlaInterfaceImpl.connectionLost: Lost Connection because: {}", faultDescription);
    }

    public void createMunitionDetonation(MunitionDetonation munitionDetonation) throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError {
        try {
            ParameterHandleValueMap theParameters = ambassador.getParameterHandleValueMapFactory().create(14);

            MunitionDetonationCoder munitionDetonationCoder = new MunitionDetonationCoder(encoderFactory);
            munitionDetonationCoder.setValues(munitionDetonation);

            theParameters.put(articulatedPartDataHandle, munitionDetonationCoder.getArticulatedPartsDataArray().toByteArray());
            theParameters.put(detonationLocParameterHandle, munitionDetonationCoder.getDetonationLocation().toByteArray());
            theParameters.put(detonationResultParameterHandle, munitionDetonationCoder.getDetonationResultCodeEnum8().toByteArray());
            theParameters.put(eventIdParameterHandle, munitionDetonationCoder.getEventIdCoder().toByteArray());
            theParameters.put(firingObjIdParameterHandle, munitionDetonationCoder.getFiringObjIdCoder().toByteArray());
            theParameters.put(finalVelVectorParameterHandle, munitionDetonationCoder.getVelVectorStructCoder().toByteArray());
            theParameters.put(fuseTypeParameterHandle, munitionDetonationCoder.getFuseTypeEnum16().toByteArray());
            theParameters.put(munitionObjIdParameterHandle, munitionDetonationCoder.getMunitionObjIdCoder().toByteArray());
            theParameters.put(munitionTypeParameterHandle, munitionDetonationCoder.getMunitionTypeStructCoder().toByteArray());
            theParameters.put(quantityFiredParameterHandle, munitionDetonationCoder.getQuantityFired().toByteArray());
            theParameters.put(rateOfFireParameterHandle, munitionDetonationCoder.getRateOfFire().toByteArray());
            theParameters.put(relativeDetonationParameterHandle, munitionDetonationCoder.getRelativePositionStructCoder().toByteArray());
            theParameters.put(targetObjIdParameterHandle, munitionDetonationCoder.getTargetObjIdCoder().toByteArray());
            theParameters.put(warheadTypeParameterHandle, munitionDetonationCoder.getWarheadTypeEnum16().toByteArray());

            ambassador.sendInteraction(munitionDetonationInteractionClassHandle, theParameters, null);

        } catch (InteractionClassNotPublished | InteractionParameterNotDefined | InteractionClassNotDefined e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        }
    }

    private URL[] loadFomModules(String pathToFomDirectory) {
        List<URL> urls = new ArrayList<>();
        File dir = null;
        try {
            dir = new File(pathToFomDirectory);
        } catch (NullPointerException e) {
            logger.error("No path to FOM directory provided. Check \"fom\" path in config file.", e);
            System.exit(0);
        }

        // Fill a list of URLs.
        File[] dirListing = dir.listFiles();
        if (dirListing != null) {
            for (File child : dirListing) {
                try {
                    urls.add(child.toURI().toURL());
                } catch (MalformedURLException e) {
                    logger.error("File not found at url : {}", child.toURI(), e);
                }
            }
        }

        // Convert the List<URL> to URL[]
        return urls.toArray(new URL[urls.size()]);
    }
}
