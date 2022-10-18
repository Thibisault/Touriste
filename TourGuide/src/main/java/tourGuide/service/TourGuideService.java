package tourGuide.service;

import com.jsoniter.output.JsonStream;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;
import tourGuide.JsonController.NearByAttractions;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TourGuideService {
	private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
			tracker = new Tracker(this);
			addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
	
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	//synchronized pour éviter le bug de concurrence quand je lance le test de performance de localisation
	public VisitedLocation trackUserLocation(User user) {
		Locale.setDefault(Locale.US);
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	public List<NearByAttractions> getNearByAttractions(VisitedLocation visitedLocation) {
		RewardCentral rewardCentral = new RewardCentral();
		List<NearByAttractions> nearByAttractionsList = new ArrayList<NearByAttractions>();
		List<Attraction> nearbyAttractions = new ArrayList<>();
		TreeMap<Double, Attraction> sortedListByDistance = new TreeMap<>();
		for(Attraction attraction : gpsUtil.getAttractions()) {
			if(rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
				NearByAttractions nearByAttractions = new NearByAttractions();
				nearByAttractions.setAttractionName(attraction.attractionName);
				nearByAttractions.setDistanceInMiles(rewardsService.getDistance(attraction, visitedLocation.location));
				nearByAttractions.setRewardsPoint(rewardCentral.getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId));
				nearByAttractions.setTouristAttractionLongitude(attraction.longitude);
				nearByAttractions.setTouristAttractionLatitude(attraction.latitude);
				nearByAttractions.setUserLongitude(visitedLocation.location.longitude);
				nearByAttractions.setUserLatitude(visitedLocation.location.latitude);
				nearByAttractionsList.add(nearByAttractions);
				nearbyAttractions.add(attraction);
			} else {
				sortedListByDistance.put(rewardsService.getDistance(attraction ,visitedLocation.location), attraction);
			}
		}
			while (nearbyAttractions.size() < 5){
				Double distance = sortedListByDistance.firstKey();
				nearbyAttractions.add(sortedListByDistance.get(distance));
				NearByAttractions nearByAttractions = new NearByAttractions();
				nearByAttractions.setAttractionName(sortedListByDistance.get(distance).attractionName);
				nearByAttractions.setDistanceInMiles(rewardsService.getDistance(sortedListByDistance.get(distance), visitedLocation.location));
				nearByAttractions.setRewardsPoint(rewardCentral.getAttractionRewardPoints(sortedListByDistance.get(distance).attractionId, visitedLocation.userId));
				nearByAttractions.setTouristAttractionLongitude(sortedListByDistance.get(distance).longitude);
				nearByAttractions.setTouristAttractionLatitude(sortedListByDistance.get(distance).latitude);
				nearByAttractions.setUserLongitude(visitedLocation.location.longitude);
				nearByAttractions.setUserLatitude(visitedLocation.location.latitude);
				nearByAttractionsList.add(nearByAttractions);
				sortedListByDistance.remove(distance);
		}
		return nearByAttractionsList;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		        tracker.stopTracking();
		      } 
		    }); 
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			generateUserPreference(user);

			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private void generateUserPreference(User user){
		CurrencyUnit currency = Monetary.getCurrency("USD");
		UserPreferences userPreferences = new UserPreferences();

		userPreferences.setAttractionProximity(new Random().nextInt(200)+1);
		userPreferences.setTripDuration(new Random().nextInt(15)+1);
		userPreferences.setTicketQuantity(new Random().nextInt(15)+1);
		userPreferences.setNumberOfChildren(new Random().nextInt(6)+1);
		userPreferences.setNumberOfAdults(new Random().nextInt(6)+1);
		userPreferences.setLowerPricePoint(Money.of(0, currency));
		userPreferences.setHighPricePoint(Money.of(new Random().nextInt(50000)+500, currency));
		user.setUserPreferences(userPreferences);
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
	
}
