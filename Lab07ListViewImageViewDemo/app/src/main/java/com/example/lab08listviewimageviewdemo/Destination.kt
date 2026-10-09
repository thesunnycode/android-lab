package com.example.lab08listviewimageviewdemo

import androidx.annotation.DrawableRes

data class Destination(
    val name: String,
    val tagline: String,
    val category: String,
    val description: String,
    @DrawableRes val thumbnailRes: Int,
    @DrawableRes val heroRes: Int
)

object DestinationRepository {

    fun all(): List<Destination> = listOf(
        Destination(
            name = "Manali, Himachal Pradesh",
            tagline = "Snow-capped peaks & pine valleys",
            category = "ADVENTURE",
            description = "Manali sits in the Kullu Valley at the base of the Pir Panjal range, popular for " +
                "trekking, paragliding, and river rafting on the Beas. Solang Valley and Rohtang Pass draw " +
                "visitors for snow sports through winter and early spring.",
            thumbnailRes = R.drawable.img_manali,
            heroRes = R.drawable.img_manali_hero
        ),
        Destination(
            name = "Goa",
            tagline = "Golden beaches & Portuguese heritage",
            category = "BEACH",
            description = "Goa's coastline stretches across calm bays and lively beach strips, backed by " +
                "whitewashed churches and spice plantations. Old Goa's basilicas and the Saturday night " +
                "markets in the north are a short drive from any beach shack.",
            thumbnailRes = R.drawable.img_goa,
            heroRes = R.drawable.img_goa_hero
        ),
        Destination(
            name = "Jaipur, Rajasthan",
            tagline = "The Pink City's forts & palaces",
            category = "HERITAGE",
            description = "Jaipur's walled old city is laid out in a grid, its buildings painted terracotta pink. " +
                "Amber Fort, City Palace, and Hawa Mahal showcase Rajput and Mughal architecture, while the " +
                "bazaars around Johari Bazaar are known for gems and block-printed textiles.",
            thumbnailRes = R.drawable.img_jaipur,
            heroRes = R.drawable.img_jaipur_hero
        ),
        Destination(
            name = "Alleppey, Kerala",
            tagline = "Backwaters & houseboat cruises",
            category = "RELAXATION",
            description = "Alleppey's network of canals, lagoons, and lakes is best explored aboard a kettuvallam " +
                "houseboat, drifting past paddy fields and coconut groves. The annual Nehru Trophy Boat Race " +
                "draws crowds to the Punnamada backwaters every August.",
            thumbnailRes = R.drawable.img_kerala,
            heroRes = R.drawable.img_kerala_hero
        ),
        Destination(
            name = "Leh-Ladakh",
            tagline = "High-altitude desert & monasteries",
            category = "ADVENTURE",
            description = "Ladakh's cold desert landscape sits above 3,000 metres, ringed by the Karakoram and " +
                "Himalaya ranges. Pangong Lake, Nubra Valley, and centuries-old monasteries like Thiksey make " +
                "it a favourite for road trips and high-altitude trekking.",
            thumbnailRes = R.drawable.img_ladakh,
            heroRes = R.drawable.img_ladakh_hero
        ),
        Destination(
            name = "Rishikesh, Uttarakhand",
            tagline = "River rafting & yoga on the Ganges",
            category = "ADVENTURE",
            description = "Rishikesh sits where the Ganges leaves the Himalayan foothills, known equally for " +
                "white-water rafting and its yoga ashrams. The Lakshman Jhula and Ram Jhula suspension bridges " +
                "link the town's two riverbanks.",
            thumbnailRes = R.drawable.img_rishikesh,
            heroRes = R.drawable.img_rishikesh_hero
        )
    )
}
