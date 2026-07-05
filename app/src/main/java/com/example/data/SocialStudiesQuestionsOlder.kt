package com.example.data

object SocialStudiesQuestionsOlder {
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
        yearSource = "BECE 2021"
    )

    private fun theory(
        qNum: Int,
        topic: String,
        qText: String,
        ans: String,
        scheme: String,
        year: String,
        marks: Int = 10
    ) = Question(
        subjectId = 4,
        type = "theory",
        topic = topic,
        difficulty = "medium",
        questionText = "Question $qNum\n\n$qText",
        optionA = null, optionB = null, optionC = null, optionD = null,
        correctAnswer = null,
        explanation = "BECE $year Social Studies Paper 2 Question $qNum",
        modelAnswer = ans,
        markingScheme = scheme,
        totalMarks = marks,
        yearSource = "BECE $year"
    )

    val questions = listOf(
        // === 2021 OBJECTIVES ===
        obj("The arm of government responsible for the making of laws in Ghana is the", "Executive.", "Legislature.", "Judiciary.", "Cabinet.", "B", "Government & Citizenship", "The Legislature (Parliament) is constitutionally empowered to draft, debate, and pass laws for national governance."),
        obj("Who is the head of the Judiciary arm of government in Ghana?", "The President", "The Speaker of Parliament", "The Chief Justice", "The Attorney General", "C", "Government & Citizenship", "The Chief Justice heads the Judiciary, which comprises the Supreme Court, Appeal Court, High Court, and lower courts."),
        obj("The movement of people from rural areas to urban centres in search of jobs is called", "emigration.", "immigration.", "rural-urban drift.", "seasonal migration.", "C", "Population & Social Issues", "Rural-urban drift is the movement of human populations from rural villages to cities in search of better living standards."),
        obj("The statutory government agency responsible for assessing and collecting national taxes in Ghana is the", "Ghana Audit Service.", "Ghana Revenue Authority (GRA).", "Bank of Ghana.", "Ministry of Finance.", "B", "Economy & Development", "The GRA is the centralized government body tasked with mobilizing domestic revenue, customs duties, and internal taxes."),
        obj("A social unit consisting of a father, mother, and children is known as", "extended family.", "nuclear family.", "clan group.", "community union.", "B", "Population & Social Issues", "The nuclear family is the immediate social unit comprising two parents and their children, distinct from the extended family."),
        obj("The main benefit of using solar energy instead of wood fuel is that it", "is cheaper to buy.", "reduces air pollution and forest depletion.", "is easier to harvest.", "provides more heat.", "B", "Our Environment", "Solar energy is clean and renewable, protecting forests and keeping the atmosphere free of harmful smoke."),
        obj("Which of the following describes the zenith of the sun directly above the equator?", "Solstice", "Equinox", "Eclipse", "Tropic", "B", "Our Physical Environment", "An equinox is the time when the sun is directly above the equator, making day and night of equal length."),
        obj("The first capital of the Gold Coast under British colonial rule was", "Accra.", "Kumasi.", "Cape Coast.", "Tamale.", "C", "History & Colonization", "Cape Coast was the initial seat of British administration before it was officially moved to Accra in 1877."),
        obj("Which of the following bodies resolves disputes between individuals and groups in Ghana?", "The Police", "The Military", "The Judiciary (Courts)", "The Parliament", "C", "Government & Citizenship", "The Judiciary has the exclusive constitutional power to settle civil and criminal disputes under the law."),
        obj("The primary reason for celebrating traditional festivals in Ghana is to", "eat delicious foods.", "remember ancestral heritages and plan local developments.", "show off traditional dresses.", "pour libations.", "B", "Culture & Tourism", "Festivals are held to honor ancestors, reunite families, and design community development policies."),
        obj("Which of the following represents a non-renewable natural resource?", "Solar energy", "River Volta water", "Crude oil", "Forest timber", "C", "Our Environment", "Crude oil cannot be replenished once exhausted, making it a non-renewable energy source."),
        obj("An advantage of a partner business enterprise over a sole proprietorship is", "unlimited liability.", "shared business risks and larger capital raising.", "private decision making.", "lower tax rates.", "B", "Economy & Development", "Partnerships allow multiple individuals to pool financial capital and share operational risks."),
        obj("Which of the following is a physical hazard?", "Corruption", "Flooding", "Inflation", "Illiteracy", "B", "Our Environment", "Flooding is a natural physical hazard that destroys farmlands, roads, and human structures."),
        obj("The Greenwich meridian (0° longitude) and the Equator (0° latitude) meet in the", "Atlantic Ocean (Gulf of Guinea).", "Pacific Ocean.", "Indian Ocean.", "Sahara Desert.", "A", "Our Physical Environment", "The Prime Meridian and Equator intersect in the Gulf of Guinea off the coast of West Africa."),
        obj("Adolescent pre-marital sex is highly discouraged in Ghana because it leads to", "high educational achievements.", "early pregnancy, school dropouts, and STIs.", "stronger family bonds.", "better community health.", "B", "Population & Social Issues", "Unprotected teenage sex causes unwanted pregnancies and health risks, leading to school dropouts."),
        obj("The first President of the Fourth Republic of Ghana was", "Dr. Hilla Limann.", "Jerry John Rawlings.", "John Agyekum Kufuor.", "John Evans Atta Mills.", "B", "History & Colonization", "Flt. Lt. Jerry John Rawlings was elected as the first President of the Fourth Republic in the 1992 elections."),
        obj("Which of the following is a fundamental obligation of a citizen of Ghana?", "Voting in all local elections", "Owning real estate", "Paying taxes promptly", "Attending political rallies", "C", "Government & Citizenship", "Paying national taxes promptly is a mandatory constitutional duty to fund national developments."),
        obj("The system of inheritance where children belong to their mother's lineage is called", "patrilineal.", "matrilineal.", "bilateral.", "monogamous.", "B", "Culture & Tourism", "In matrilineal systems (common in Akans), descent and inheritance trace along the maternal line."),
        obj("Which of the following maps will show the borders of countries in West Africa?", "Physical map", "Political map", "Climate map", "Topographical map", "B", "Map Work & Practical Skills", "Political maps are specifically designed to highlight sovereign country borders, regions, and capitals."),
        obj("The most common cause of bushfires in Ghana during the dry season is", "high solar heat.", "lightning strikes.", "irresponsible human activities like slash-and-burn farming.", "volcanic eruptions.", "C", "Our Environment", "Uncontrolled slash-and-burn clearing and hunting are the leading causes of dry season bushfires."),

        // === 15 OLDER THEORY QUESTIONS ===
        theory(1, "Our Environment",
            "(a) Define environmental degradation.\n(b) Explain three human activities that cause environmental degradation in Ghana.\n(c) Suggest two ways by which environmental degradation can be controlled.",
            "(a) Environmental degradation is the deterioration of the physical environment through depletion of resources such as air, water, and soil, leading to ecological destruction.\n\n(b) Three human causes:\n1. Illegal Mining (Galamsey): Excavates pits, destroys fertile agricultural land, and contaminates rivers with dangerous chemicals like mercury.\n2. Deforestation (Lumbering): Uncontrolled felling of forest trees for timber dries up streams and destroys wildlife habitats.\n3. Improper Waste Disposal: Littering plastic materials and discharging untreated waste block gutters and contaminate soil and underground water tables.\n\n(c) Two solutions:\n1. Strict Enforcement of Environmental Laws: Prosecuting offenders of illegal mining and logging.\n2. Public Education: Sensitizing citizens on recycling, afforestation, and sanitary practices.",
            "Definition: 2 Marks\n3 Causes: 6 Marks\n2 Solutions: 2 Marks\nTotal: 10 Marks", "2022"),

        theory(2, "History & Colonization",
            "(a) State three factors that led to the 1948 Accra Riots.\n(b) Explain the role of the Watson Commission in Ghana's struggle for independence.",
            "(a) Three factors leading to the 1948 Accra Riots:\n1. The Shooting of Ex-Servicemen: Superintendant Imray fired on peaceful veteran protesters marching to Christianborg Castle on February 28.\n2. Boycott of European Goods: Organized by Nii Kwabena Bonne III to protest high consumer prices.\n3. High Unemployment: Ex-soldiers received no pensions or packages, creating extreme economic frustration.\n\n(b) Role of the Watson Commission:\nThe Watson Commission investigated the riots and recommended major political changes, declaring the previous Burns Constitution outdated. It suggested a constitutional draft committee (the Coussey Committee), which eventually laid the legal path to full self-government and elections in 1951.",
            "3 Factors: 6 Marks\nCommission role: 4 Marks\nTotal: 10 Marks", "2021"),

        theory(3, "Government & Citizenship",
            "(a) Who is a citizen?\n(b) State three constitutionally mandated duties of a Ghanaian citizen.\n(c) Identify two fundamental human rights guaranteed under the 1992 Constitution.",
            "(a) A citizen is a person who is legally recognized as a member of a country, enjoying constitutional rights, protections, and duties.\n\n(b) Three duties of a Ghanaian citizen:\n1. Obeying national laws and respecting the authority of courts.\n2. Paying taxes, rates, and duties promptly to fund national development.\n3. Protecting public property and combating corruption.\n\n(c) Two fundamental human rights:\n1. Right to Life and Liberty: Freedom from arbitrary arrest.\n2. Right to Education: Equal access to primary and secondary education.",
            "Definition: 2 Marks\n3 Duties: 6 Marks\n2 Rights: 2 Marks\nTotal: 10 Marks", "2023"),

        theory(4, "Population & Social Issues",
            "(a) Explain what is meant by rural-urban drift.\n(b) State three negative effects of rural-urban drift on cities (urban centres) in Ghana.\n(c) Suggest two measures to curb rural-urban drift.",
            "(a) Rural-urban drift is the movement of human population from rural farming villages to urban centers or cities in search of better living standards and employment.\n\n(b) Three negative effects on cities:\n1. Growth of Slums: Overcrowded informal settlements emerge without pipe water or sanitation.\n2. High Crime Rates: Lack of jobs forces desperate migrants into robbery, scamming, and sex trade.\n3. Pressure on Facilities: Severe strain on hospital beds, schools, public transport, and waste systems.\n\n(c) Two remedies:\n1. Rural Industrialization: Setting up cottage factories (One District One Factory) to create rural jobs.\n2. Decentralization of Services: Providing quality hospitals, universities, and recreation centers in district communities.",
            "Explanation: 2 Marks\n3 Effects: 6 Marks\n2 Remedies: 2 Marks\nTotal: 10 Marks", "2020"),

        theory(5, "Economy & Development",
            "(a) State three reasons why tourism is considered an important economic sector in Ghana.\n(b) Suggest two ways the government can improve the tourism industry in Ghana.",
            "(a) Three reasons why tourism is important:\n1. Foreign Exchange Earning: Tourists spend foreign currencies, strengthening the cedi.\n2. Direct and Indirect Jobs: Employs tour guides, hotel staff, transport operators, and local artisans.\n3. Cultural Preservation: Encourages local communities to maintain historical forts, festivals, and national parks.\n\n(b) Two ways to improve tourism:\n1. Infrastructure Upgrades: Constructing paved roads leading to tourism sites (e.g., Kakum National Park, Wli Waterfalls).\n2. Intrepid Marketing campaigns: Promoting Ghana globally as the peaceful hub of West African historical tourism.",
            "3 Reasons: 6 Marks\n2 Improvement measures: 4 Marks\nTotal: 10 Marks", "2019"),

        theory(6, "Government & Citizenship",
            "(a) Distinguish between the three organs of government in Ghana.\n(b) Explain the term 'Separation of Powers'.",
            "(a) The three organs are:\n1. The Executive: Led by the President and Cabinet, who execute laws and administer the state.\n2. The Legislature: The Parliament, comprised of MPs, who debate and make laws.\n3. The Judiciary: The court system, led by the Chief Justice, which interprets and applies laws to settle disputes.\n\n(b) Separation of Powers is a political doctrine stating that the three organs of government must function independently with separate powers and responsibilities, preventing any single organ from holding absolute power.",
            "Organs breakdown: 6 Marks\nSeparation of Powers explanation: 4 Marks\nTotal: 10 Marks", "2022"),

        theory(7, "Population & Social Issues",
            "(a) Explain three functions of the family as a basic social institution.\n(b) Compare matrilineal and patrilineal systems of inheritance in Ghana.",
            "(a) Three functions of the family:\n1. Procreation: Ensuring human continuity through birth.\n2. Socialization: Teaching moral values, language, and culture to children.\n3. Security and Economic Support: Providing food, shelter, clothes, and financial security to members.\n\n(b) Matrilineal vs Patrilineal systems:\nIn the Matrilineal system, children belong to their mother's clan, and inheritance of property/stools flows along the mother's side (common in Akans). In the Patrilineal system, children belong to their father's clan, and succession/inheritance flows through the paternal line (common in Ewes, Ga-Adangmes, and Northern groups).",
            "3 Functions: 6 Marks\nComparison: 4 Marks\nTotal: 10 Marks", "2018"),

        theory(8, "Social & Global Relations",
            "(a) Identify three common causes of ethnic conflicts in Ghana.\n(b) State two ways by which ethnic conflicts can be avoided to promote national unity.",
            "(a) Three causes of ethnic conflicts:\n1. Chieftaincy Disputes: Disagreements over succession to ancestral skins/stools.\n2. Land Ownership Battles: Rival tribes contesting border lines of fertile farmlands.\n3. Political Exploitation: Politicians using ethnic sentiments to secure votes, creating tribal rivalry.\n\n(b) Two ways to promote national unity:\n1. Promoting Inter-Ethnic Marriages: Fosters integration, dissolving prejudices.\n2. National Cultural festivals: celebrating diversity and promoting tolerance.",
            "3 Causes: 6 Marks\n2 Solutions: 4 Marks\nTotal: 10 Marks", "2021"),

        theory(9, "Our Physical Environment",
            "(a) Distinguish between the rotation and revolution of the Earth.\n(b) Explain one effect of the Earth's rotation and one effect of the Earth's revolution.",
            "(a) Earth rotation is the spinning of the Earth on its own axis from West to East, completing one turn in 24 hours. Earth revolution is the orbital movement of the Earth around the Sun, completing one full orbit in 365.25 days.\n\n(b) Effects:\n1. Rotation Effect (Day and Night): As the Earth spins, the half facing the Sun experiences daylight, while the opposite half is in darkness.\n2. Revolution Effect (Changes in Seasons): The tilt of the Earth's axis combined with its orbit around the Sun results in differing solar intensity and seasonal cycles (Summer, Winter, Spring, Autumn).",
            "Differentiating concepts: 4 Marks\nEffects explanation: 6 Marks\nTotal: 10 Marks", "2022"),

        theory(10, "Social & Global Relations",
            "(a) Explain what is meant by international cooperation.\n(b) Outline three economic benefits Ghana derives from being a member of ECOWAS.",
            "(a) International cooperation is the collaboration of independent sovereign nations to work together to solve shared challenges, promote trade, security, and healthcare.\n\n(b) Three benefits of ECOWAS to Ghana:\n1. Free Movement of People: Ghanaians can travel and trade across 15 West African states without visa requirements.\n2. Larger Export Market: Local companies can export tariff-free to a market of over 300 million people.\n3. Regional Security: Cooperation in intelligence and military peacekeeping (ECOMOG) secures political stability in the sub-region.",
            "Explanation: 2 Marks\n3 Benefits: 6 Marks\nTotal: 10 Marks", "2023"),

        theory(11, "Government & Citizenship",
            "(a) Identify three national symbols of Ghana.\n(b) Explain the significance of each of the identified national symbols.",
            "(a) Three national symbols of Ghana:\n1. The National Flag\n2. The Coat of Arms\n3. The State Sword (Okyeame Poma / State Scepter)\n\n(b) Significance:\n1. The National Flag: Red represents the blood shed for independence; Gold represents mineral wealth; Green represents forest vegetation; Black Star represents African freedom.\n2. The Coat of Arms: Symbolizes government authority, military protection, and the national motto 'Freedom and Justice'.\n3. The State Sword: Symbolizes traditional state authority, democratic power, and allegiance of leaders to the citizens.",
            "3 Symbols: 3 Marks\nSignificance explanation: 7 Marks\nTotal: 10 Marks", "2021"),

        theory(12, "History & Colonization",
            "(a) State three positive and three negative political impacts of colonization on Ghana.",
            "(a) Three positive political impacts:\n1. Introduction of unified, modern judicial and court systems.\n2. Integration of diverse local tribes into one sovereign nation-state with demarcated borders.\n3. Development of formal local government systems (civil service and district councils).\n\n(b) Three negative political impacts:\n1. Complete loss of sovereignty and independent traditional self-governance.\n2. Undermining and weakening of traditional authorities (chiefs and queenmothers).\n3. Partitioning of families and ethnic groups due to artificial geographical boundaries.",
            "3 Positive impacts: 5 Marks\n3 Negative impacts: 5 Marks\nTotal: 10 Marks", "2020"),

        theory(13, "Economy & Development",
            "(a) Differentiate between secondary and tertiary production.\n(b) Give two examples of industries under secondary and two under tertiary production in Ghana.",
            "(a) Difference:\n- Secondary production involves processing raw materials into finished or semi-finished physical goods (manufacturing and assembly).\n- Tertiary production provides essential support services to individuals, businesses, and industries (does not produce physical products).\n\n(b) Examples:\n- Secondary: Cocoa processing factories (e.g. Golden Tree), cement factories (e.g. Ghacem), breweries, textile mills.\n- Tertiary: Banking, education, telecommunications (e.g. MTN), tourism, healthcare.",
            "Difference: 4 Marks\nExamples (2 each): 6 Marks\nTotal: 10 Marks", "2019"),

        theory(14, "Population & Social Issues",
            "(a) Explain what is meant by puberty.\n(b) Outline three ways adolescents can protect themselves from irresponsible moral behavior.",
            "(a) Puberty is the physiological transition period during which a child's body undergoes rapid physical, biological, and hormonal changes to reach sexual maturity and capability of reproduction.\n\n(b) Three protective measures:\n1. Peer Selection: Avoiding peer groups that indulge in drugs, alcoholism, or early sexual acts.\n2. Focus on Education: Channels mental and physical energy into schoolwork, sports, and productive clubs.\n3. Open Communication: Discussing physical and emotional changes regularly with trustworthy parents or school counselors.",
            "Puberty definition: 4 Marks\n3 Protective ways: 6 Marks\nTotal: 10 Marks", "2018"),

        theory(15, "Social & Global Relations",
            "(a) What are Non-Governmental Organizations (NGOs)?\n(b) Describe three contributions of NGOs to community development in Ghana.",
            "(a) Non-Governmental Organizations (NGOs) are non-profit, voluntary, and independent citizen groups organized on local, national, or international levels to address humanitarian, social, and developmental issues.\n\n(b) Three contributions of NGOs:\n1. Healthcare Support: Providing free medical screenings, building clinics, and distributing drugs (e.g. red cross).\n2. Quality Education: Building rural schools, donating books, and offering scholarships to needy students (e.g. Camfed).\n3. Clean Water Supply: Constructing boreholes and water purification stations in remote villages to prevent water-borne diseases.",
            "NGO definition: 4 Marks\n3 Contributions: 6 Marks\nTotal: 10 Marks", "2020")
    )
}
