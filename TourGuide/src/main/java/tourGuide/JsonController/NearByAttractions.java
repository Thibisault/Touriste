package tourGuide.JsonController;

import lombok.Data;

@Data
public class NearByAttractions {

    String attractionName;
    Double touristAttractionLongitude;
    Double touristAttractionLatitude;
    Double userLongitude;
    Double userLatitude;  
    Double distanceInMiles;
    Integer rewardsPoint;

}
