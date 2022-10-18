package tourGuide.helper;

        import java.util.ArrayList;
        import java.util.List;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;
        import java.util.concurrent.TimeUnit;

        import lombok.Data;
        import org.apache.commons.lang3.time.StopWatch;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import tourGuide.service.TourGuideService;
        import tourGuide.tracker.Tracker;
        import tourGuide.user.User;

@Data
public class TrackerHighVolumeTrackLocation extends Thread {
    private final Logger logger = LoggerFactory.getLogger(Tracker.class);
    private final TourGuideService tourGuideService;
    private boolean stop = false;
    private List<User> userTreatement = new ArrayList<>();

    public void setUserTreatement(List<User> sublistUserTreatement) {
        this.userTreatement.addAll(new ArrayList<User>(sublistUserTreatement));
    }

    public TrackerHighVolumeTrackLocation(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    @Override
    public void run() {
        StopWatch stopWatch = new StopWatch();
        logger.info("Nombre de User dans la liste = " +userTreatement.size());
        while(true) {
            if (Thread.currentThread().isInterrupted() || stop) {
                logger.debug("Tracker stopping (1)");
                break;
            }

            logger.debug("Begin Tracker. Tracking " + userTreatement.size() + " users.");
            stopWatch.start();
            userTreatement.forEach(u -> tourGuideService.trackUserLocation(u));
            stopWatch.stop();
            logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
            stopWatch.reset();
            break;
        }
    }
}
