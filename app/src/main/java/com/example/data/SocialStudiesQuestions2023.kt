package com.example.data

object SocialStudiesQuestions2023 {
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
        yearSource = "BECE 2023"
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
        explanation = "BECE 2023 Social Studies Paper 2 Question $qNum",
        modelAnswer = ans,
        markingScheme = scheme,
        totalMarks = marks,
        yearSource = "BECE 2023"
    )

    val questions = listOf(
        // === OBJECTIVES ===
        obj("Which of the following best promotes the smooth operation of Separation of Powers in Ghana?", "Presidency", "Chief Justice", "Constitution", "Speaker of parliament", "C", "Government & Citizenship", "The Constitution defines the distinct powers of each branch and sets checks and balances."),
        obj("To prevent human rights abuse in Ghana, the court must", "Allow their settings to be telecast by TV stations", "Be fair to all manner of persons", "Extend their sitting hours to late evening", "Set up a supreme court in every district", "B", "Government & Citizenship", "The judiciary must practice fairness and equality before the law to protect fundamental rights."),
        obj("Which of the following cultural practices should be maintained in Ghana?", "Incision of tribal marks", "Widowhood rights", "Trokosi", "Child naming", "D", "Culture & Tourism", "Naming a child is a positive and celebratory cultural event, unlike other harmful customs listed."),
        obj("Ghana is bordered to the east by which of the following countries?", "Togo", "Cote d'Ivoire", "Burkina Faso", "Nigeria", "A", "Our Physical Environment", "Ghana shares its eastern border with the Republic of Togo."),
        obj("Which of the following is a good measure for preventing human rights violation in Ghana?", "Extending formal education to all", "Organizing free and fair elections", "Making external travel more accessible", "Establishing more human rights institutions", "D", "Government & Citizenship", "Institutions like CHRAJ directly monitor, investigate, and prevent human rights abuses."),
        obj("In which type of rock can coal be found?", "Igneous rock", "Volcanic rock", "Sedimentary rock", "Plutonic", "C", "Our Physical Environment", "Coal is formed from organic remains compressed under rock layers, which occurs in sedimentary formations."),
        obj("Which of the following cultural practices is considered acceptable in the Ghanaian society?", "Trokosi", "Dipo", "Female Genital Mutilation", "Widowhood rights", "B", "Culture & Tourism", "Dipo is a traditional puberty rite of the Krobo people which has high cultural acceptance and educational value."),
        obj("One of the founding fathers of Ghana who left the United Gold Coast Convention (U.G.C.C) to form his own party was", "J.B Danquah.", "Kwame Nkrumah.", "Ako Adjei.", "Nana William Ofori Atta.", "B", "History & Colonization", "Kwame Nkrumah broke away from the UGCC on June 12, 1949, to form the Convention People's Party (CPP)."),
        obj("The main duty of the police in a community is to", "Arrest criminal", "Intimidate people", "Direct traffic", "Keep law and order", "D", "Government & Citizenship", "The primary, overarching mandate of the police service is to maintain public law and order."),
        obj("A society is said to be disciplined when its members", "Work hard to promote the well-being of the society", "Obey the rules and the regulations that govern the society", "Agree to choose their leaders very peacefully", "Are friendly, especially towards foreigners.", "B", "Population & Social Issues", "A disciplined society is defined by collective obedience and conformity to established rules and laws."),
        obj("Which one of the following factors was not a cause of the 1948 riots in the Gold Coast?", "Nkrumah’s suspension from the U.G.C.C.", "Rising prices of goods", "Mass unemployment", "General discontent against European rule", "A", "History & Colonization", "Nkrumah's suspension from the UGCC happened *after* the riots had started and political tensions grew."),
        obj("Which of the following latitudes divides the world into two equal halves?", "0º", "23 1/2ºN", "23 1/2ºS", "66 1/2ºN", "A", "Our Physical Environment", "The Equator (0 degrees latitude) divides the globe into the Northern and Southern Hemispheres."),
        obj("Which of the following investment cover one’s properties against fire outbreaks?", "Treasury bill", "Government bonds", "Insurance policy", "Company shares", "C", "Economy & Development", "An insurance policy provides financial indemnity and covers properties against accidental fire or damage."),
        obj("Which of the following is not an indigenous Ghanaian language?", "Dagare", "Ga", "Nzema", "Hausa", "D", "Culture & Tourism", "Hausa originated in Niger/Nigeria and is a lingua franca in West Africa, but is not native/indigenous to Ghana."),
        obj("Which of the following rivers flow into Lake Volta?", "Afram", "Pra", "Densu", "Tano", "A", "Our Physical Environment", "The Afram river directly drains into the Volta Lake basin."),
        obj("The two main types of cooperation that exist among nations are", "Bilateral and Cultural", "Bilateral and multilateral", "Economic and technical", "Educational and multilateral.", "B", "Government & Citizenship", "The two main diplomatic structures of cooperation are bilateral (two nations) and multilateral (many nations)."),
        obj("Which place did the Akans settle first during their migration to their present locations?", "Denkyira", "Dormaa Ahinkro", "Bono Manso", "Chiraa", "C", "History & Colonization", "The Akans settled first at Bono Manso around Techiman before expanding further south."),
        obj("Which of the following areas in Ghana is most likely to experience relief rainfall?", "Volta lowlands", "Accra plains", "Northern lowlands", "Kwahu scarp", "D", "Our Physical Environment", "High relief structures like the Kwahu scarp force moist winds upward, creating relief (orographic) rainfall."),
        obj("Lake Bosomtwe, which is one of the natural lakes in the world is located in the", "Ashanti Region.", "Brong-Ahafo Region.", "Central Region.", "Eastern Region.", "A", "Our Physical Environment", "Lake Bosomtwe, a natural meteor crater lake, is located in the Ashanti Region of Ghana."),
        obj("One of the many ways by which conflicts are reduced in the society is through", "Prayers", "Meditation", "Mediation", "Punishment", "C", "Population & Social Issues", "Mediation is an active, structured dispute resolution process involving an impartial third party."),
        obj("Ghana’s dependence on foreign loans can be minimized by", "Diversifying and processing more for exports", "The importation of skilled labour", "The size of her population", "Printing more money", "A", "Economy & Development", "Processing raw commodities into high-value exports raises revenue, reducing need for international loans."),
        obj("The imaginary lines shown on maps which help to determine time are called", "Longitudes", "Equator", "Latitudes", "Great circles", "A", "Map Work & Practical Skills", "Longitudes (meridians) are used to calculate time differences, with 15 degrees representing 1 hour."),
        obj("The largest ethnic group in Ghana is the", "Ga-Adangmes.", "Ewes.", "Guans.", "Akans.", "D", "Population & Social Issues", "The Akan ethnic group is the largest in Ghana, comprising roughly 47% of the national population."),
        obj("A bill becomes a law when it is signed by the", "Chief Justice", "Attorney General", "President", "Speaker of parliament", "C", "Government & Citizenship", "In Ghana's democracy, presidential assent is the final constitutional step to turn a passed bill into a law."),
        obj("The local government system is meant to", "Provide resources for all", "Elect leaders", "Support the traditional justice system", "Bring government closer to the people", "D", "Government & Citizenship", "Decentralization and local assemblies aim to involve grassroot citizens in decision-making and development."),
        obj("A disciplined child is the one who", "Is well educated in the family", "Is well trained in the family", "Obeys the rules of the family", "Organizes the family for communal labour.", "C", "Population & Social Issues", "A disciplined child demonstrates self-control and obeys household and family rules."),
        obj("Which of the following towns is a manganese producing centre in Ghana?", "Nkwatia", "Nsuta", "Saltpond", "Mpraeso", "B", "Economy & Development", "Nsuta in the Western Region of Ghana is a prominent, world-class center for manganese mining."),
        obj("Europeans presence and activities on the Gold Coast included the following except", "Trading with the people", "Evangelizing the people", "Destroying farm lands for gold", "Spreading European civilization", "C", "History & Colonization", "Europeans built forts, traded, built schools and churches, but did not mine gold using modern farmland destruction."),
        obj("Primary production is important in Ghana because it provides", "Credit", "Capital", "Employment", "Services", "C", "Economy & Development", "Primary activities (farming, fishing, logging) employ over 40% of the active working population in Ghana."),
        obj("Which of the following did not form part of the reasons for the formation of the Fante Confederation?", "Promote education of the people on their rights", "Protest against the Poll Tax Ordinance", "Protect the people from external attack", "Provide social amenities for the people", "A", "History & Colonization", "The confederation was to defend against Ashanti invasions and coordinate local development, not teach human rights."),
        obj("International sporting activities do not only serve to promote friendship but also to", "Generate income for the countries", "Win trophies", "Receive aids and grants", "Improve cultural practices", "A", "Government & Citizenship", "Hosting major sports (e.g. AFCON) brings in tourists, generating significant economic income (WAEC Key)."),
        obj("A scale presented in a line form is known as", "Map scale", "Statement scale", "Representative scale", "Linear scale", "D", "Map Work & Practical Skills", "A graphical scale representing map distances in the form of a marked-out line is a linear scale."),
        obj("Irresponsible behaviour of adolescents can result in", "Child abuse", "Responsible parenting", "Peer pressure", "School dropout", "D", "Population & Social Issues", "Irresponsible sexual acts or drug use by adolescents often leads to teen pregnancy or school dropout."),
        obj("The southwest monsoon winds, more often than not bring rainfall to the southern parts of Ghana between", "December and March", "January and April", "February and June", "April and October", "D", "Our Physical Environment", "The wet southwest monsoon season in southern Ghana typically runs from April to October."),
        obj("The Greenwich meridian passes through one of the following places.", "Accra", "Sunyani", "Tema", "Ho", "C", "Our Physical Environment", "The Prime Meridian (0 degrees longitude) passes directly through the industrial port city of Tema."),
        obj("If the time at town (A) on longitude 20°E is 9:00 a.m., what will be the time at town (B) on longitude 80°E?", "1:00 p.m.", "1:00 a.m.", "8:00 p.m.", "8:00 a.m.", "A", "Map Work & Practical Skills", "Difference is 80 - 20 = 60 degrees. 60 degrees * 4 mins = 240 mins = 4 hours. Town B is east (+), so 9am + 4h = 1:00pm."),
        obj("The main source of light energy on the Earth is the", "Hydro", "Moon", "Sun", "Stars", "C", "Our Physical Environment", "The Sun is the ultimate and principal source of natural heat and light energy on the Earth."),
        obj("One factor that can make the adolescent drop out of school is", "Chaste life", "Early parenthood", "Social dignity", "Moral discipline", "B", "Population & Social Issues", "Teen pregnancy and early parenthood force many boys and girls out of school to look for work/childcare."),
        obj("In the solar system, the Earth is one of the", "Globes", "Latitudes", "Longitudes", "Planets", "D", "Our Physical Environment", "The Earth is one of the eight major planets orbiting the Sun in the solar system."),
        obj("Universal Adult Suffrage guarantees the right to", "Respect the laws of the land", "Pay taxes on time", "Vote to elect leaders", "Register as a citizen", "C", "Government & Citizenship", "Universal Adult Suffrage ensures every adult citizen (18+) has the fundamental constitutional right to vote."),

        // === THEORY ===
        theory(1, "Our Physical Environment",
            "List four features of a good map.\nCalculate the time at town 'A' on longitude 45° E if the time at town 'B' on longitude 0° is 9 a.m.\nExplain five ways in which rocks are useful to humans.",
            "(a) Four features of a good map:\n1. Title: Explains what the map shows.\n2. Scale: Shows the ratio/relationship between map and actual ground distances.\n3. Key/Legend: Explains the symbols, colors, and markers used on the map.\n4. Direction (North Arrow): Displays map orientation.\n\n(b) Time calculation:\n1. Longitude difference = 45° - 0° = 45°.\n2. Time difference = 45° * 4 minutes = 180 minutes = 3 hours.\n3. Since Town 'A' is East (45° E) of Town 'B' (0°), its time is ahead (+).\n4. Time at A = 9:00 a.m. + 3 hours = 12:00 noon (12:00 p.m.).\n\n(c) Five ways rocks are useful to humans:\n1. Construction and Building: Rocks like granite, sandstone, and limestone are crushed for roads, homes, and bridges.\n2. Sources of Minerals: Rocks contain highly valuable industrial and precious minerals like gold, diamonds, and bauxite.\n3. Tourist Attractions: Unique geomorphological rock formations (e.g. Umbrella Rock) attract tourists, yielding revenue.\n4. Energy Source: Sedimentary coal and petroleum-bearing shale rocks provide fossil fuel energy.\n5. Soil Formation: Weathering of rocks breaks them down into soil particles rich in minerals for crop farming.",
            "Map features: 4 Marks\nTime calculation (showing steps): 6 Marks\nFive ways rocks are useful: 10 Marks\nTotal: 20 Marks", 20),

        theory(2, "Our Environment",
            "Outline four measures that can be taken to reduce the pollution of water bodies in Ghana.\nIdentify four major problems that rural areas suffer from as a result of the migration of youth to the cities.",
            "(a) Four measures to reduce water pollution in Ghana:\n1. Strict enforcement of environmental laws and bye-laws against illegal mining (galamsey).\n2. Treatment of all domestic sewage and industrial chemical waste before discharge into rivers.\n3. Public educational campaigns to raise awareness on the dangers of throwing plastics and solid wastes into water bodies.\n4. Promoting the use of organic manure instead of dangerous chemical fertilizers near river banks.\n\n(b) Four problems rural areas face from youth migration to cities:\n1. Low Food Production: The physically active youth migrate, leaving farming to children and the elderly, reducing crop yields.\n2. Decline in Local Crafts: Traditional handicraft and weaving skills are lost as the next generation moves away.\n3. Severe Shortage of Labour: Lack of communal workforce for developmental projects like roads, clinics, and building schools.\n4. Broken Homes: Separation of family structures leads to lack of parental supervision for children left behind, causing delinquency.",
            "Measures to reduce water pollution: 8 Marks\nRural migration problems: 12 Marks\nTotal: 20 Marks", 20),

        theory(3, "Government & Citizenship",
            "State the three arms of government in Ghana and highlight three functions of the District Chief Executive (DCE) under local government.\nOutline four reasons why Ghana enters into cooperation with other countries.",
            "(a) (i) Three arms of government in Ghana:\n- The Executive, The Legislature, and The Judiciary.\n\n(ii) Three functions of the District Chief Executive (DCE):\n1. Serves as the chief representative of the central government in the local district.\n2. Oversees the day-to-day general administration of the District Assembly.\n3. Maintains law, order, security, and peaceful coexistence within the district.\n\n(b) Four reasons why Ghana enters into international cooperation:\n1. Technical Cooperation: To share skills, educational tools, technology, and professional expertise.\n2. Fighting Common Problems: Collaborating to solve cross-border epidemics, climate change, and food security.\n3. Security and Defense: Partnering with other nations (e.g. ECOWAS, UN) for peacekeeping and protection against terrorism.\n4. Expanding Markets: Opening wider tariff-free trade zones to export local Ghanaian manufactured and agricultural products.",
            "Arms of government: 6 Marks\nDCE functions: 6 Marks\nCooperation reasons: 8 Marks\nTotal: 20 Marks", 20),

        theory(4, "Government & Citizenship",
            "Differentiate between freedom and obligation and list four obligations of a citizen to the state.\nHighlight four ways in which law and order are maintained in schools.",
            "(a) (i) Difference between Freedom and Obligation:\n- Freedom refers to the fundamental human rights, civil liberties, and privileges enjoyed by a citizen under the national constitution (e.g., freedom of movement).\n- Obligation refers to the legal, civic, and moral duties that a citizen is constitutionally required to perform for the state (e.g., paying taxes).\n\n(ii) Four obligations of a citizen:\n1. Paying taxes, rates, and duties regularly to fund national infrastructure.\n2. Respecting and obeying all national laws and constitution.\n3. Protecting and preserving state properties and assets.\n4. Reporting criminal activities and cooperating with security agencies.\n\n(b) Four ways law and order is maintained in schools:\n1. Enforcement of School Rules: Strict administration of rewards and punishments to guide student conduct.\n2. Respect for Authority: Students obeying instructions of teachers, school administration, and student prefects.\n3. Guidance and Counseling: Providing moral uprightness, discipline counseling, and psychological guidance to students.\n4. Teacher Supervision: Constant monitoring of student behavior in classrooms, dormitories, and dining halls by teachers on duty.",
            "Freedom vs Obligation: 4 Marks\nCitizen obligations: 4 Marks\nSchool law and order: 12 Marks\nTotal: 20 Marks", 20),

        theory(5, "Our Environment",
            "What is a human settlement? Explain four reasons why bad layouts of settlements must be prevented.\nHighlight four ways in which the forest vegetation of Ghana can be preserved.",
            "(a) (i) A human settlement is a place or geographic space where people have established their homes and live or reside.\n\n(ii) Four reasons why bad layouts must be prevented:\n1. Easy Movement: To ensure emergency vehicles (fire trucks, ambulances) and residents can move freely without obstruction.\n2. Utility Supply: To make it easy to lay critical water pipes, sewage systems, and electrical power cables.\n3. Sanitation: To prevent the outbreak and fast spread of water-borne and air-borne diseases by ensuring proper waste disposal paths.\n4. Crime Control: Orderly layouts allow security patrols and police vehicles to patrol the neighborhood efficiently.\n\n(b) Four ways to preserve the forest vegetation in Ghana:\n1. Creating Forest Reserves: Restricting human logging, farming, and construction in protected ecosystems.\n2. Afforestation: Planting new trees in deforested areas to replace those cut down.\n3. Enforcing Strict Environmental Laws: Imposing heavy jail terms and fines on illegal loggers and tree fellers.\n4. Fire Belts: Clearing boundary strips around forests to block the spread of devastating bushfires during dry Harmattan.",
            "Settlement definition: 4 Marks\nPreventing bad layouts: 4 Marks\nForest preservation: 12 Marks\nTotal: 20 Marks", 20),

        theory(6, "Economy & Development",
            "State the two classifications of human resource.\nOutline four measures the government can take to ensure the efficient use of human resource in Ghana.",
            "(a) Two classifications of human resource:\n1. Skilled human resource: Academically and technically trained professionals (e.g. doctors, teachers, engineers).\n2. Unskilled human resource: Laborers with little to no formal technical training who perform manual and routine work (e.g. cleaners, porters).\n\n(b) Four measures the government can take to ensure efficient human resource use:\n1. Providing quality vocational and technical education (TVET) to train school-leavers with practical industrial skills.\n2. Improving Working Conditions: Ensuring safe work spaces, health insurance, and adequate welfare to minimize brain drain.\n3. Attractive Remuneration: Offering competitive salary scales and bonuses to motivate workers to put in their best.\n4. Regular In-service Training: Sponsoring workshops and skills upgrading refreshers to update civil servants on new technologies.",
            "Human resource classifications: 4 Marks\nMeasures for efficient use: 16 Marks\nTotal: 20 Marks", 20)
    )
}
