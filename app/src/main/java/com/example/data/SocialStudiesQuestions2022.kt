package com.example.data

object SocialStudiesQuestions2022 {
    private fun obj(
        q: String,
        a: String, b: String, c: String, d: String,
        ans: String,
        topic: String,
        expl: String
    ) = Question(
        subjectId = 4,
        type = "objective",
        topic = topic,
        difficulty = "medium",
        questionText = q,
        optionA = a, optionB = b, optionC = c, optionD = d,
        correctAnswer = ans,
        explanation = expl,
        modelAnswer = null,
        markingScheme = null,
        totalMarks = 1,
        yearSource = "BECE 2022"
    )

    private fun theory(
        qNum: Int,
        topic: String,
        qText: String,
        ans: String,
        scheme: String,
        marks: Int = 10
    ) = Question(
        subjectId = 4,
        type = "theory",
        topic = topic,
        difficulty = "medium",
        questionText = "Question $qNum\n\n$qText",
        optionA = null, optionB = null, optionC = null, optionD = null,
        correctAnswer = null,
        explanation = "BECE 2022 Social Studies Paper 2 Question $qNum",
        modelAnswer = ans,
        markingScheme = scheme,
        totalMarks = marks,
        yearSource = "BECE 2022"
    )

    val questions = listOf(
        // === OBJECTIVES ===
        obj("Which of the following can aid economic independence of a nation?", "Giving more aid to needy countries", "Generating enough revenue locally", "Increasing imported goods", "Defending the 1992 constitution", "B", "Economy & Development", "Generating enough revenue locally ensures self-reliance and reduces reliance on foreign assistance."),
        obj("Which of the following does not constitute a right of a Ghanaian according to the 1992 constitution?", "Right to unlawful assembly", "Right to personal liberty", "Right to life", "Freedom of Association", "A", "Government & Citizenship", "The constitution protects lawful and peaceful assembly, not unlawful assembly."),
        obj("The application of science to improve upon the quality of life or work is also known as", "education", "technology", "computers", "machine", "B", "Science & Technology", "Technology is the practical application of scientific knowledge for practical purposes."),
        obj("The following activities bring disgrace to the people and Ghana as a whole except", "the chieftaincy institution.", "female genital mutilation.", "armed robbery.", "galamsey mining.", "A", "Culture & Tourism", "Chieftaincy is a respected traditional institution, whereas other options are harmful or illegal."),
        obj("Which of the following activities is a duty of a citizen of Ghana?", "Attending religious meetings", "Instigating mass boycott of development", "Owning huge property", "Defending the 1992 constitution", "D", "Government & Citizenship", "Defending the constitution of Ghana is a fundamental civic duty of every citizen."),
        obj("Which of the following measures cannot improve the quality of life in the rural areas of Ghana?", "Unregulated family planning", "Extending good roads to the rural communities", "Greater access to improved formal education", "Establishment of industries in the rural areas", "A", "Population & Social Issues", "Unregulated family planning does not improve rural life quality, whereas roads, school, and industry do."),
        obj("The early introduction of formal education by the colonialists in the Gold Coast, led to", "improvement in foreign trade.", "improvement in civil service.", "increase in labour force.", "improvement in crop production.", "A", "History & Colonization", "Early formal education was initially designed to train clerks and improve administrative/foreign trade interactions."),
        obj("The following are aspects of culture except", "beliefs", "ceremony", "language", "food", "B", "Culture & Tourism", "While ceremony is custom, beliefs, language, and food are basic continuous elements of cultural systems (WAEC Key)."),
        obj("The growth rate of Ghana's population can be reduced mainly through", "family planning.", "legislation", "public education.", "abortion", "A", "Population & Social Issues", "Family planning allows couples to space births and limit family size safely, lowering growth rate."),
        obj("The revolution of the earth around the sun causes", "day and night.", "deflection of winds and ocean currents.", "the four seasons.", "the rise and fall of the tides.", "C", "Our Physical Environment", "The Earth's revolution around the Sun (with its tilted axis) causes the seasonal cycles of the year."),
        obj("The scale on the map is 1:200,000. If the distance on the map is 2 cm, find the actual distance on the ground?", "1.2 km", "20 km", "2 km", "4 km", "D", "Map Work & Practical Skills", "2 cm on map = 2 * 200,000 cm = 400,000 cm = 4 km actual distance on the ground."),
        obj("Why are laws made?", "For family unity", "For conformity", "For instant justice", "For development", "B", "Government & Citizenship", "Laws are primarily created to maintain social order and ensure people conform to established rules of conduct."),
        obj("Which of the following is not an element of the weather?", "Wind", "Humidity", "Tide", "Thermometer", "D", "Our Physical Environment", "Thermometer is an instrument used to measure temperature, not an element of the weather itself."),
        obj("The safest place to save money is the", "susu", "bank", "credit union", "money box", "B", "Economy & Development", "Commercial banks are fully regulated and offer the highest financial security for saving money."),
        obj("Increasing opportunities for employment in the Ghanaian society can help to reduce", "acts of indiscipline", "the cost of living.", "responsibility to the state.", "influx of foreign materials into the country.", "A", "Economy & Development", "Providing jobs keeps youth productive, drastically reducing idle time and acts of social indiscipline."),
        obj("Which of the following activities show interaction between the physical and social environments?", "A father advising the child", "A mother breastfeeding the baby", "Man listening to music", "Elephants drinking water from the river", "D", "Our Environment", "Elephants (living organisms) drinking water (physical environment) is a direct eco-interaction (WAEC Key)."),
        obj("The Ghana Coat of Arms was designed by", "Amon Kotei", "Philip Gbeho", "Ephraim Amu", "Sophia Doku", "A", "Government & Citizenship", "Amon Kotei was the legendary Ghanaian artist who designed the national Coat of Arms."),
        obj("An account which is operated with the use of cheque, is known as", "Current account", "Moneybox savings", "Post office savings", "Savings account", "A", "Economy & Development", "A current account is a transactional bank account that allows payments via cheques."),
        obj("One of the ways of promoting unity among the ethnic groups in Ghana is through", "Christian and Islamic rights", "consulting the gods", "adult education", "inter-ethnic marriages", "D", "Population & Social Issues", "Inter-ethnic marriages connect families across different tribes, significantly building ethnic unity."),
        obj("Which principle is used to check abuse of power in governance?", "Rule of law", "Separation of powers", "Executive Veto", "Parliamentary immunity", "B", "Government & Citizenship", "Separation of powers ensures no single branch gains dictatorial control of government."),
        obj("The savanna vegetation zones produce cattle because of the", "Fulani herdsmen", "extensive grassland", "hot climate", "abundant rainfall", "B", "Our Physical Environment", "Savanna vegetation is dominated by extensive grasses which provide rich grazing fodder for cattle."),
        obj("The process of identifying signals of conflict and encouraging people to work out their differences is known as", "resolution", "prevention", "involvement", "management", "D", "Population & Social Issues", "Conflict management involves identifying conflict early and guiding parties to peacefully address differences."),
        obj("Which of the following factor can best make private sector businesses more profitable", "Availability of ready market", "Construction of improved roads", "Stability in supply of adequate electricity", "Adequate supply of labour", "A", "Economy & Development", "Having a ready market is the ultimate driver of sales, leading directly to higher business profitability."),
        obj("The colour blue is used on topographical map to indicate", "buildings", "rivers and lakes", "mountains", "rocks", "B", "Map Work & Practical Skills", "Blue on physical maps represents water features, including rivers, seas, and lakes."),
        obj("Which of the following does not reflect the importance of festivals in Ghana?", "Plans are made for developmental projects", "Chiefs are enstooled", "Family members come together", "Disputes are encouraged", "D", "Culture & Tourism", "Festivals are held to settle disputes and foster peace, not to encourage disputes."),
        obj("One factor that can affect the academic performance of students negatively is", "effective supervision", "loitering and truancy", "prefects", "co-curricula activities", "B", "Population & Social Issues", "Truancy and loitering take students away from learning, directly worsening academic performance."),
        obj("Laws in the country are made by the", "parliament", "Government", "Judiciary", "District assembly", "A", "Government & Citizenship", "The Legislature (Parliament) has the constitutional authority to draft and enact laws for the country."),
        obj("The statement scale 1cm to 5km can be converted to representative fraction to read", "1 : 50,000", "1 : 500,000", "1 : 50", "1 : 5,000", "B", "Map Work & Practical Skills", "5 km = 5 * 1000m * 100cm = 500,000 cm. Thus, the ratio scale is 1 : 500,000."),
        obj("The health of the people of Ghana can be improved through the", "supply of electricity", "application of proper environmental sanitation activities", "expanding non-formal education", "setting up of industries", "B", "Population & Social Issues", "Proper sanitation prevents disease vectors, drastically improving public health."),
        obj("For Ghanaians to come out of negative influence of colonial mentality, they should", "develop high sense of self-reliance.", "stop foreigners from coming into Ghana.", "develop taste for European goods.", "adopt foreign cultures.", "A", "History & Colonization", "Developing a high sense of self-reliance reduces the mental dependency left by colonial rule."),
        obj("Which of the following levels is imposed by the District Assemblies in Ghana?", "Income tax", "Driving Licenses", "Market tolls", "Court fines", "C", "Government & Citizenship", "District Assemblies impose local levies like market tolls and property rates to fund local development projects."),
        obj("The person who combines the factors of production such as land, labour and capital is called", "shop keeper", "operation", "producer", "entrepreneur", "D", "Economy & Development", "An entrepreneur is the visionary who organizes, coordinates, and manages land, labour, and capital."),
        obj("In Ghana festivals usually serve all the following purposes except", "outdooring of new-born babies", "community development planning", "providing entertainment", "pouring of libations to the ancestors", "A", "Culture & Tourism", "Outdooring is a family-level naming rite, whereas community festivals serve public/ancestral planning functions."),
        obj("Which of the following is associated with a scale of a map", "Linear", "Tilted", "Compass", "Balanced", "A", "Map Work & Practical Skills", "A linear scale is a common graphical scale showing distances represented on a map line."),
        obj("The sole proprietor enjoys all the following advantages except", "high profits", "privacy", "quick decisions", "unlimited liability", "D", "Economy & Development", "Unlimited liability is a major disadvantage of sole proprietorship, meaning personal assets can be seized for debt."),
        obj("Which of the following factors is one of the major reasons for Ghana's cultural diversity", "Colonization", "Migration", "Foreign trade", "internal trade", "B", "Culture & Tourism", "Historic migration of different ethnic groups into Ghana brought about its diverse cultures and languages."),
        obj("Which of the following accounts for the presence of the equatorial forest in south western Ghana?", "Lumbering", "Afforestation", "Cloud cover", "Rainfall", "D", "Our Physical Environment", "Heavy, double-maxima rainfall in southwestern Ghana sustains the luxurious equatorial rainforest."),
        obj("Food production in Ghana faces the problem of", "foreign exchange", "post-harvest losses", "rainfall", "loss of trees", "B", "Economy & Development", "Poor storage facilities cause massive post-harvest losses, reducing net food availability in Ghana."),
        obj("Which of the following measures can increase tomato yield in Ghana", "Improved marketing", "Availability of credit to farmers", "Rainfall", "Processing factories", "B", "Economy & Development", "Giving credit to farmers lets them purchase improved seeds, fertilizer, and irrigation to boost yields."),
        obj("Ghana's export are highly dominated by", "capital goods", "primary products", "finished goods", "crude exports", "B", "Economy & Development", "Ghana's exports consist primarily of unprocessed raw primary products like cocoa, gold, and timber."),

        // === THEORY ===
        theory(1, "Our Physical Environment",
            "In the space below, draw an outline of the globe. Mark and label the following features:\n(i) Arctic Circle\n(ii) Tropic of Capricorn\n(iii) Latitude 0° (Equator)\n(iv) The direction of the Earth’s rotation\n(v) The North Pole.\n\nAlso, state two major highlands and two major lowlands in Ghana, and outline two uses of the International Date Line (IDL).",
            "(a) Draw an outline of the globe: Draw a neat circle representing the Earth.\n\n(b) Labels:\n- Arctic Circle: Dashed line at 66.5° N.\n- Tropic of Capricorn: Dashed line at 23.5° S.\n- Latitude 0° (Equator): Horizontal line in the center.\n- Direction of rotation: West to East (left to right) arrow near equator.\n- North Pole: Point at the very top (90° N).\n\n(c) Highlands & Lowlands:\n- Highlands: Akwapim-Togo Ranges, Kwahu Plateau, Mampong Scarp, Wa Scarp, Gambaga Scarp.\n- Lowlands: Volta Basin, Coastal Plains, Densu/Pra/Tano River Basins.\n\n(d) Uses of the International Date Line (IDL):\n- Avoids time confusion across the world by establishing a standard place where the calendar date changes.\n- Helps travelers recognize when a calendar day is lost or gained when crossing east or west.",
            "Globe Drawing & Labels: 5 Marks\nHighlands/Lowlands listing: 4 Marks\nIDL uses explanation: 4 Marks\nTotal: 13 Marks", 13),

        theory(2, "Population & Social Issues",
            "Define the term 'population'.\nGiven country X's table:\n- 0 - 17: 10,815,000 (54.4%)\n- 18 - 59: 8,836,000\n- 60+: 948,400 (3.6%)\nCalculate the working population percentage and the total population of Country X.\nOutline four disadvantages of Ghana's population structure.",
            "(a) Definition of Population:\nPopulation is the total number of people living in a particular geographical area at a specific period of time.\n\n(b) Calculations:\n(i) Percentage of Working Population (18-59 years):\nWorking% = 100% - (54.4% + 3.6%) = 100% - 58% = 42%.\n\n(ii) Total Population of Country X:\nTotal = 10,815,000 + 8,836,000 + 948,400 = 20,599,400.\n\n(c) Four disadvantages of Ghana's youthful population structure:\n1. High Dependency Ratio: Large number of children puts severe economic pressure on the small working population.\n2. High Government Expenditure: Most public revenues are spent on basic social services (schools, healthcare) instead of industries.\n3. Low Savings and Investment: Personal incomes are spent raising children, resulting in little capital for investment.\n4. High Unemployment: Job creation can't match the speed of the growing youth population entering the workforce.",
            "Population definition: 4 Marks\nWorking% (showing work): 2 Marks\nTotal Population: 2 Marks\n4 Disadvantages: 12 Marks\nTotal: 20 Marks", 20),

        theory(3, "Social & Global Relations",
            "Explain the following types of cooperation:\n(i) Political Cooperation\n(ii) Cultural Cooperation\nHighlight four ways Ghanaians can maintain their national unity.",
            "(a) Types of Cooperation:\n(i) Political Cooperation: This is when sovereign nations join together or sign treaties to advance their political interests and maintain peace (e.g. joining UN, AU, Commonwealth).\n(ii) Cultural Cooperation: This is a collaborative exchange program where countries share their arts, heritage, sporting, and educational skills to learn from one another (e.g. hosting AFCON, PANAFEST).\n\n(b) Four ways Ghanaians can maintain national unity:\n1. Promoting Inter-Ethnic Marriages to bridge family divisions across tribes.\n2. Ensuring fair and equal distribution of national resources and developmental projects across all regions.\n3. Teaching indigenous music, dance, and histories of various ethnic groups in primary/junior schools.\n4. Encouraging political tolerance, dialogue, and respect for divergent views among citizens.",
            "Cooperation explanations: 8 Marks\nFour ways to maintain unity: 12 Marks\nTotal: 20 Marks", 20),

        theory(4, "Culture & Tourism",
            "List four major ethnic groups in Ghana. Describe the migration routes of the Akans and Ewes into Ghana, and explain three factors that promoted these ethnic migrations.",
            "(a) Four major ethnic groups in Ghana:\n- Akan, Ewe, Ga-Adangbe, Guan, Mole-Dagbani.\n\n(b) Migration routes:\n- Akans: Believed to have migrated from the ancient Ghana Empire, entered Ghana through the Black Volta river basin, settled first at Bono Manso/Techiman, then expanded into the forest belts and southern coast.\n- Ewes: Traced their ancestry from Oyo in Nigeria, migrated through Ketu (Benin), settled in Tado and Notsie in Togo, and fled from the oppressive King Agorkoli to settle around Keta lagoon and southern Volta region in three distinct groups.\n\n(c) Three factors that promoted ethnic migrations:\n1. Search for fertile farming lands and fresh water bodies for fishing and drinking.\n2. Escape from natural disasters such as famine, droughts, or outbreaks of epidemic diseases.\n3. Escape from tribal wars, external invasions, or cruel and oppressive rulers.",
            "Listing groups: 4 Marks\nAkan/Ewe routes: 10 Marks\nFactors promoting migration: 6 Marks\nTotal: 20 Marks", 20),

        theory(5, "Our Environment",
            "Define: (i) Human settlement, (ii) A slum.\nHighlight four benefits a person gains from touring.\nIdentify four uses of land in your community.",
            "(a) Definitions:\n(i) Human settlement is a place or geographic space where people establish their homes and live or reside.\n(ii) A slum is an overcrowded, filthy, and poorly planned urban area characterized by lack of basic social amenities and disorganized buildings.\n\n(b) Four benefits of touring:\n1. Provides rest, enjoyment, and mental relaxation after long periods of tedious work.\n2. Expands knowledge by exposing the traveler to new cultures, traditions, and historical sites.\n3. Promotes physical fitness and health through outdoor hiking, walking, and clean air.\n4. Enables the acquisition of unique cultural artifacts, souvenirs, and craft items.\n\n(c) Four uses of land in communities:\n1. Residential purposes (building houses, shelters, and homes).\n2. Agricultural purposes (cultivating food crops and livestock rearing).\n3. Transport infrastructure (constructing highways, railways, and feeder roads).\n4. Commercial and industrial purposes (building marketplaces, retail shops, and factories).",
            "Definitions: 4 Marks\nTouring benefits: 8 Marks\nLand uses: 8 Marks\nTotal: 20 Marks", 20),

        theory(6, "Economy & Development",
            "List two examples of primary economic industries in Ghana.\nOutline three major problems facing primary economic industries in Ghana.\nExplain four measures that can be taken to improve primary economic industries.",
            "(a) Two examples of primary economic industries in Ghana:\n- Cocoa farming, gold mining, timber logging, marine fishing.\n\n(b) Three major problems facing primary industries:\n1. Heavy reliance on erratic weather and natural rainfall patterns, leading to crop failures during droughts.\n2. Lack of credit facilities and high interest rates preventing farmers from acquiring modern machinery.\n3. Severe post-harvest losses due to absence of cold storage facilities and processing factories near farming hubs.\n\n(c) Four improvement measures:\n1. Constructing irrigation dams and reservoirs to reduce reliance on seasonal rains.\n2. Providing highly subsidized credit, fertilizer, and agricultural machinery to local farmers.\n3. Building modern storage silos, warehouses, and cold hubs across rural communities.\n4. Establishing localized processing factories (One District One Factory) to process raw materials into high-value goods.",
            "Primary industries listing: 2 Marks\nProblems facing industries: 6 Marks\nImprovement measures: 12 Marks\nTotal: 20 Marks", 20)
    )
}
