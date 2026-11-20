package com.granit.survival

/**
 * Original Granit Survival pixel art, generated specifically for this project.
 * Player frames are authored at 32x48; environment objects remain 16x16.
 * Slash-separated rows keep the atlas compact and easy to validate.
 */
internal object GeneratedPixelSprites {
    private val palette = mapOf(
        'A' to floatArrayOf(0.91f, 0.84f, 0.72f),
        'B' to floatArrayOf(0.70f, 0.63f, 0.52f),
        'C' to floatArrayOf(0.60f, 0.54f, 0.43f),
        'D' to floatArrayOf(0.56f, 0.40f, 0.27f),
        'E' to floatArrayOf(0.44f, 0.38f, 0.29f),
        'F' to floatArrayOf(0.35f, 0.32f, 0.27f),
        'G' to floatArrayOf(0.35f, 0.24f, 0.17f),
        'H' to floatArrayOf(0.28f, 0.23f, 0.19f),
        'I' to floatArrayOf(0.22f, 0.18f, 0.15f),
        'J' to floatArrayOf(0.13f, 0.11f, 0.085f),
        'K' to floatArrayOf(0.085f, 0.065f, 0.045f),
        'L' to floatArrayOf(0.028f, 0.016f, 0.009f),
        'M' to floatArrayOf(0.36f, 0.43f, 0.32f),
        'N' to floatArrayOf(0.16f, 0.21f, 0.17f),
        'O' to floatArrayOf(0.28f, 0.34f, 0.29f),
        'P' to floatArrayOf(0.45f, 0.37f, 0.27f),
        'Q' to floatArrayOf(0.78f, 0.48f, 0.19f),
        'R' to floatArrayOf(0.61f, 0.63f, 0.52f),
        'S' to floatArrayOf(0.10f, 0.13f, 0.12f),
        'T' to floatArrayOf(0.82f, 0.72f, 0.53f), // cloth edge highlight
        'U' to floatArrayOf(0.18f, 0.27f, 0.23f), // webbing and straps
        'V' to floatArrayOf(0.77f, 0.39f, 0.19f), // signal orange detail
        'W' to floatArrayOf(0.43f, 0.57f, 0.53f), // steel hardware
        'X' to floatArrayOf(0.08f, 0.12f, 0.12f)  // deep seam
    )

    private fun sprite(vararg frames: String) = PixelSprite(
        frames = frames.map { it.split('/') },
        palette = palette
    )

    private val base = mapOf(
        "player" to sprite(
            ".......LL......./....IKGHIIK...../...LHEGGFEHKJ.../...LHEEDEDGIJ.../..LJHGGGGHHIL.../..KKIJLKJJJKFK../.JBGGBKADLCABE../..KELAJAAABLFJ../.LCHFKAAAAKFJII./.CCJFEGFEFEIECJ./ICCIGJFCEEIGFBD./LDEJEEJFJFBGIDCL/LEFLEDCECCDFLGDL/LEGKFDCFDCDHKFDL/LACIDGIJHFDELDAL/.BBLJCDGFDDKLCAI/...LIHKLLLHHFLL./...LFEEHJEEEF.../...LHEIILFEH..../...LHHHJKLKJ..../...HJHKJLGJHH.../..HHKGIJHHHHH.../...HKCGJHHHH..../....HLHHHH......",
            ".......LL......./...LJIGIHHL...../...LHEEHGGGK..../...LFGDFEFGII.../..LIIGGFGGHJK.../..KLIJJGECIKL.../..HILLLBLLDDFL../..JHLACAAAALH.../.LCIEDAAABLEJEK./.DBFFFGJGHFKECE./LDCEGJECCEIGGCCL/LDEJHCHLLEBHJCDL/LDDJFECFECCGJEEL/LEEJGDCEDBDHKFEL/.BDGEHFGEFEFLACL/.LBLKJDDEEHKLBB./...IHEHHIHIL..../...HFEEKIHH...../....HFFKLKL...../....LHJLIII...../...HLEIKLGKF..../...HLIGKHHHHH.../...HHLLHHHHHH.../.....HHHHH......",
            ".....JJKKJJ...../....KHFHIIIJ..../...LHGFFFFGJK.../...LHEEHDDHHK.../...JIHHFFHHIK.../...LJLDKJLJLGJ../...KKAJAAGBBDK../...KIAAAAAAKIJ../..LJFLAAAAICJDL./.KCDIFHJJIEIGBF./.EDCGIJEEIEGHCE./LCCHICCIIBCGKDEK/LCELEECEECCEJGCL/LECLIECCDDEIKFCL/.KAAKEFEFFDGKBAL/.KDBLLKFFJKILDAK/...FKHHIIHCH.LL./....KHFJJEEL..../.....JIIKECH..../....HIEHIEFK..../....HHLJLHIHH.../....HHHHJGGHH.../.....HHHKDEFH.../.........LL.....",
            ".......KK......./....LJGHHIK...../...KIDEFFGILK.../...LHFEEEFFJJ.../..KJHGIGHHHKI.../...LIILHIJJLL.../...IADLBKKGKDL../...JHAEACABKGI../..LKEIABAALEKEK./.JCEJFIIIFEIECF./.KCEIHEEEEIGECCL/.DCFKDIIHFCIHEEL/LEFLJDCEDCDFLHDL/LDELJDCFEBCHLGGL/IHILIEGGHGGGLBDE/.AAALDDFEDDKKAA./.JCK.HJLLIHL.LK./.....JIIIEEJ..../.....KHIHHFL..../.....KGHIFHK..../....HHLKLHIHH.../....HHHHGGJHH.../....HHHHIDIHH.../......HHJJF....."
        ),
        "player_back" to sprite(
            ".....JKIJJJ...../....LIGGHHHJ..../...IHEGGGEEIJ.../...IEFFEEEEIJ.../...KHJGGFHJIL.../..JIKJIHIIJJL.../..LLLJJIJJJLEL../...HJKLLLLLKK.../.JILECCBCCCELDK./.DCHKELLLLLJFBEL/HDCEKDCCCDCKICFL/KDCFKFDDDEELKFEL/KDDLFEGIIIDKHGH./LCDLGGEDDDGKH.../LGGLFIGFGGHIL.../KAAIKEFFGHHIL.../.KLLHHIJIHIL..../...JIHILHFEK..../....LKLLHEEL..../....LEILFFFL..../...HHKKLJFIHH.../...HHHHLEELHH.../....HHHLIILHH.../......HHLLF.....",
            "......JL......../....KIHHIHL...../..LIHFGGGEIK..../...JGEEEEGIJ..../..JKHGFFHIII..../...LIIIHIIJK..../..EJLJJIJJJJ..../..LJHKKKJJJJL.../..KIECCBCCCFFJ../.LCGJCCCCEFJDCJ./.LCCKIILLLDGFCDL/GDCHKEDEDDDEKEDL/KFGALEICECDELHFL/LIAAJFFDDDEGLBD./IHHKHKHEDGHILLK./....LFGJKLLIL.../....LIKKLKIL..../.....IILHEFL..../.....KKLHFFL..../....HJILGGIK..../...HHIIGDDKH..../...HHHHHJIKHH.../....HHHLJIIHH.../......HHHHH.....",
            "......JLJ......./....LIIGHILK..../..KIEFGGGFFJL.../..KIGEEEFHGJI.../..KIIIGFHHIJL.../...LJHIIIJIK..../.LDKKJJHIJKLD.../..JDGKLKKKKIL.../.IJKCCCBCCCFIHJ./.FCGIEGHHHIJECF./LDDDFKCBBCCJECDL/LCDHFLDEEDDKJECL/KDCLILGDDDJJLFCL/LDELHKGDDDEKLIF./LHHLIKGEGEGLLBL./KAALEFJKLLLGL.../.DDIIJKKKJJIL.../....IFHJKHEF..../....KIILIEFII.../....KIILJHHJL.../....IGILIEKLH.../...HHLLHJIIHH.../....HHHHJIIHH.../......HHILH.....",
            ".....IIKKH....../....KIGHHFIJ..../...KHEEHGGFHL.../...LGFEEFEFHL.../..KKIGGGGJIIK.../...KJIIIIIJJJ.../..LDLJJJJJJLDK../...JHKJKKJKFL.../..HLDCDBCCDELL../..CDKDCCCCEIGDK./.KCCLILLKKCEECE./.GBELEDDDEEEGBD./LDFFIEEDCDCGLEEJ/LCCIHEGGFEDFLECL/LCBKHIIEEDGHLIIL/.ABDKFGLLLLHLBAL/..LHLKJKLLJK.GG./.....KHKHEEI..../....KHIKFEIK..../....IJJKIFIJH.../....HHKHEGLHH.../....HHKEEGHHH.../....HHLJJIHHH.../......HLLKH....."
        ),
        "player_side_left" to sprite(
            "......KH......../..KKGGHIJL....../KLGEEGEGGHJ...../HKHDEHEGFHIL..../KIIKKIHIGHJK..../..ABBGJKIJJ...../..AAAAABJJK...../..JAAAJLKEEDDL../...LCGKCGGJDDGL./....HEILCGKGDDL./...LIKJDDDHGEDD./...JCBKFCCHIEEF./....ECIHCCFJGGG./....FDECDEIJHGL./....FKCDGLHJ..../...KBAAKJFHL..../...JLBDJKIIIL.../...IFHIKIHHHIL../...JFFIL.LHIII../...KIHK....LGJH./..LLGIKHHHJGGIKH/.LDGGHHHHHHLLHHI/.IKJKKHHHHHHHH../..HHHHHHHHH.....",
            "....JJHII......./.KKGFFGGIJJ...../IKGEEFFEEHJ...../JGEGGFGFGEIIL.../HIJBIHHHFIIL..../.HEKDKILJIJL..../..CCAABAJJJG..../..JAAAAILJDLDLI./...JJKLBGGGCBDJ./...EIEKLCCJEEBI./...LFFLDCDGHGBF./....HCKGCCDLEKGL/....HEFKGCELGGGL/....JJHCCEIJIHIK/....KDKCEKFHLLL./...JKAABHHHLLL../...JHLLLIIJJ..../...JEHILHHIHK.../...JKHIHLJHIGL../....LHIH..LGGGK./..JLLGILIHJGGLH./..EEGGHLHHJJJHH./..JIIILLHHHHHH../..HHHHHHHHH.....",
            "...K.LKJL......./.KKHGHHHIKJ...../KKGGGEEFFHKJ..../HJGIFGDFGFIL..../JKKAKIHHJIJL..../.LGDIKJLJIJJ..../..DAKADAKJKHH.../..HAAAAHLKEKCLI./...KLKKFCIGGCDD./...FKCFLDBJGGECI/...LFKDICCDJGEEK/...JKDDIDCCJLGGL/...KLBDFIBDILIGK/...KLDGECDEKLHHK/...DLCLIDEKHLLL./...ILKAAGHGLK.../.....HKBIIL...../.....HFJIIIK..../.....LFFIJHGL.../.....LJHJLHIL.../.....HLIILGKHHHH/....JDDGGJHHHHH./...HJGGGLHHHHH../....IHHHHHH.....",
            "....LK.LJ......./.LLHEGFGIJL...../LHGEEDFFGIIL..../JIFHFHGHFHIJ..../JJIBIIIIFIIK..../..GLGJJEJJJJ..../..EAAAADJJKKK.../..HAAABLECELDG../...IKLLCGGJDEDL./...FFEKLCCKDDEL./...LFIEECCEKDIGL/...IFCEICCELGDGL/....LCEGICDKHGIL/....LDDHKCEJJJ../....LDEIKCEJH.../...ILILLAADKL.../...HEEIKBAGJJL../...IFHHKIJHHJEJ./...LJHJJ.HJLGGGL/....LIIJ...ILGIL/....LGGKHHHJIJHH/..LDDGGKHHHHHHHH/..LGGIGJHHHHHH../...HHHHHHHH....."
        ),
        "player_side_right" to sprite(
            ".......KK......./.....JJHIGHIL.../...JKIFEFFFGGJK./...KIGGGFEGEFGJ./...IIIHHGGHEIII./...JIIHKHIGCKL../....LJJCACAHAL../..LLLLKKKAAAK.../.LCCIDEGHDAAG.../.GCDGFLLFFHF..../HECFLDCCJGFK..../IEDGLCCDJEGJ..../LIFJLCCFKDEK..../LHGJLECGKEFK..../.JIJFECCLEEK..../...KJJFEAIHJ..../...LJKLAAAHJ..../..HIFHILLKHHK.../..LLHHL.LIHFK.../..DGIJI..IIIJ.../.KGGIJ..HLIGLK../.FLIIKFFFLIGEGL./.HFFFFFFFFLLLLFF/.....FFFFFFF....",
            "........LK.J..../....IKHHGHHHJ.J./...JKHFEEFEFGJL./....IFGFHFGIHIL./...LIIFHHGJDIL../....IIJLJKBKB.../....LKJEBAAAA.../..LCFDKKDAAAI.../.JCDLDDELGBK..../.EDDIGDEIFFG..../KFDFIHCBEFIJ..../LGDGIJDCDFFJ..../LGGIGGDCDKFH..../LGGIHDHECCFG..../.KKLDDIKFEGAE.../....LJJKLKIBD.../....LIIHHHJLI.../...JHHILIIFHK.../..LEJIL..IHFH.../..IGGJ...KHIEHH./.KIGGFH...KGGGGL/.HFLLLHHFFJIIGKF/..HFFHHHFFFLLFFF/.....FFFFFFF....",
            "........IL....../.....LJGGGHHLL../...ILHFEEFHFGIL./...JHFGFEFEFGIK./...KIHHHIHGGIJJ./...JLIIJLIJDGL../....LJILBEALAL../....LILJLAAAB.../...JEFCGIEAAL.../..KCCDKLFFHG..../..IDGGCBGGHI..../.LGDKDCDGGGH..../.KGHLCCCKEDI..../KGIKLDCFGEEL..../KHGKIFCCJEEEL.../.HKLILFFAJFLI.../....LJHAAEIJ..../...LJIJLLKHHL.../..KHFHIL.LJLJKL./.JGIHIL..HLGDIGL/LGGIJ.....KIIGLF/KIGJIHFHHHHLLLFF/.IJJIHHFFHHHHFF./.....FFFFFF.....",
            "........LK....../....KLIGGIHGJL../..KJKIFFEEGFFHK./..KIHGFEEGGGFGIL/...KIHHIHGIIKIII/...LJIHIJIGCGKL./....LIIKABALAB../....LILKKAAAAI../..LELHEEHHAAL.../.JDCCKGJLCEK..../.IDDJDKEBFKK..../LIEEIDKDCDLL..../KHDIFKJECDGL..../JGGGGLGLDCCFL.../LHGHKKEGJEEEGAL./.IJKJIEEGJLEABK./...KJJKIJJJLDK../..HHFIILHHFLK.../..LJHIJLJHFFL.../.LGGLJI.KIHIJ.../KGGILJ..ILJDKKL./.LLIGLFFHLIGGGHF/.FHFFFFHFFLLLLKF/....FFFFFFFF...."
        ),
        "exit_hatch" to sprite(".JFFEEEEEEEEEEH./ICIIIIIKHIIIHIL./AIHFFFFFFFFFFFCA/AFEHFHFFFFHHHFKB/AFFFFFFFFFFFFFKB/AHJFEFFFFFFFFKKA/BFKFKCKKKKBLJJJB/AEKKKHABBBCKKKIA/AEKKJFKJJKCKKKIB/AEKKKFKKJKBKKKIB/AEKKJHAABACJKKJB/AEKKJFJKKJCKKLJA/AEKKKEKKJJEKJLJB/AKKLKKLKKKKJKLEA/FFGEFBFBDFBFBLEH/.LHHGHFHHGHFHHK."),
        "ladder_hatch" to sprite(".AECFEEEEEEEEBA./CBBLBBFBDCCCCLBB/FBBLFCBCECBCILCF/FBCLFBBBCBBBILBE/FCCAHBBCBBBBHLBE/EBCLFBBKLCBBILCE/FBBLHCBKLBBBILEE/FBBLFBFHLKLBJLDC/FBBKEHLKFKLCJLBC/FBBLECBFKBHBILFE/EBBLFBFEEHBBILCE/FBCBCBCEFCBBILBE/FBCLHCCBBBCBILBE/FBFJFBCCBCBBJLCE/ICCICCECCCBCBLBC/.HHIKJJIJJJIIHFJ"),
        "vault_door" to sprite("LABAAEBAAJABBAAK/ACBBBBCCBCBBBBBC/BBCBBBCBBFBCBCBC/LBBCLLKLLBLLLKBB/BFFBLLLLCCFHHHCB/ABCCCCCCEBBBBBEB/BBBBCCCDEACFFEEB/BBBBCBCCEBCFCFHB/BBCCJIHHCCCDFEHB/BBBFIIIIIBEHEEHB/EBBEEBFHIBCCEEHC/ACCCIEJHFAFCEFFB/BBBHHEIHILIJIIBB/ABCBBEBBBLBBBBBB/ECBBBLGHECBBBBBE/JHEIFEIIIHIFFFFI"),
        "control_console" to sprite(".ACAAAAAAABAABA./AKAAFAAAAACCAAJB/BCBBKBCBBBBJBBFB/ACBCCCCCCCCCCBFB/ACBBBBBBBBBBBBFA/ACBBBBBBBBBBBBFB/ACBBBBBBBBBBBAEB/BCBBBBBBBBBBBBEB/ACBBBBBBBBBBCBFA/ACBBBBBBBBBBBBEB/ACBBBBEFEEEBBBEA/ACBBBBEEEFBBBCEB/BCBCKEEEEFEFEBFE/AIBBHCCCBBCHBBIA/ICBECBEBFCCFBCFI/.IHFEHFFHFHGHFK."),
        "bed" to sprite("EHBBBBBBBBBCCBCH/CFAAAAAAAAAAAAFE/CHABAAAAAAAECCKE/CHAAAAAAAAAEBBLF/FHAAAAADAAAEBALF/EIAAAADDDDACAALE/BIAAAAADAAACAALE/CHAAAAAAAAACBAKE/BHAAAAAAAAACBBJE/BHAAAAAAAAACABJE/BHAAAAAAAAACBBJE/CHAAAAAAAAAJCFKE/BFAAAAAAAAAICBLH/BFAAAAAAAAAICFKI/CHBBABBAAAACAAKE/JIEEEEEEEEEEEEEL"),
        "dining_table" to sprite("...LLLL.....LLLJ/..CIHIKI...FFHIC/.IKDFFFFBDBDG..B/E.FGJJKJDDBAAI.B/H.EDAAABBBBJAFFC/FJFCEEEEFDADBFEE/FHHJDBDDBBDDGIFE/FHFGBBBBBDDAKBEC/FHFDBGBACDDEKGFC/H.FDCJBABBDGDIHE/H.FGBJBBBDBBBIHF/H.FGKJJGDGGGGJFF/KGFIHIHIIHIHHIJK/.FHHHHHHIHHIIIKK/.IHHFFFFFFFFFFF./..IHFFFF.F..FGJ."),
        "generator" to sprite("........IF....../LBBBCCBBACJBBB../BBBBBAACEBCBCB../BBLAADDCCCECCB../BBKIBBDCCHBCDC../BBACFDDJBBAIGC../BDDDAFEGBBBGFB../DCAAEEEDDCCFGBG./BBIIDBBBBBCEBBGJ/DBBBBABCABAAAAEJ/LKJJJKKKKKKJKIDI/.GGIIIIIIGIIJLBC/.DFFFFFFFFLLGLKK/.DFFFFFFFLGLILIK/.DFFFFFFFILFILFJ/............IH.."),
        "medical_station" to sprite("............FE.C/C..........IAAEC/BHHEDDEDDCEAAAAC/BBEDDDEDDDDAAAAC/BBEDDDDDDDDAAAAB/BBEDDDDDDEDAAAAC/CADDDEDDDDDAAAAC/ABDDDDDDCDDAAAAB/ABEEDDDEEECFBCBB/EBHFGFHHHFFJJIKF/EAAAAAAAAAABAAAE/FJJJJJJJJJJJIJJF/LFLLLLLLLLLLLLLL/IFFFFFFFFFFFFFK./JFFFFFFFFFFFFFK./JFFFFFFFFFEFFFK."),
        "workbench" to sprite("LECCCCCCCCCCCCC./LBEBBBAABBBBBHBL/LBBCCJLLLLLCBCBL/LBBBBFKLLLLBBBBL/LBBBBFJIHHHBBBBL/LBBBCCCBBBCCCBBL/LBBBBCCBCCCCCBBL/LBJFBCCCBCCCCBBL/LBJHCCCCCCCBBBBL/LCCCBCCBCBBCCBCL/LBBCBBCCCBCCCBBL/LBCCCCBBCBCCBBBL/LBBCCEIIIHHCCBCL/LBBCCCBBBBCCCCBL/LBFEEFFFFFFFFHCL/.LFL........JHL."),
        "locker" to sprite("GBBBBBBBBBBBABBD/DDDDDDDDDDDDDDDD/EKEFHJEIFJGIIFJG/ELAEACBDBEGGDBKE/EJBEAEBDCCLCFCKD/EJKDLDICLGDGEKIE/EJJKLJKKLLIKLIJE/EKBDHHLJGIGBDCJE/ELBDJILJGBDBGDJE/DKLCJKLEDDDLDLIE/GKJLLKLKLLLKLLJE/EKADBJKJIGDBBCID/EKADCJLIGGDDCCID/GKIEIJLGDEKGGJIE/GIEDDDDDDDDDDDGD/.L.F...F....GGLH"),
        "bookshelf" to sprite("KLLLLLLLLLLLLLL./DKBABBBBBBABBBLK/DLBDDGDDDDDDDBLD/DLAGDDDDDDGDGBLD/DLADBBBCBBBBCBLD/DKBCCCCCCCBCBALC/DJCCDCDDDDDDDBLB/DJIGGGGEEGGEEELC/CJCBCDDCDDDCDDKB/DKCIKKKKKKLJKCLD/EKCEDCDDDDCDDCLC/DJCHBCGDDDDDGCLD/EKCDDLDCBCIEGBLD/EKCDDDGCDGDCGCLD/GJDIGGCDHDGGGBLD/.KGIGEGIIGGIIEL."),
        "crate" to sprite("..............K./ICCGBBBBBBBJBLE./ICBACCBKCBBJDCB./IBDFDJDKBBBJEBI./JLFGDHDJBBHJEBJ./JCCCCDBDBCHKEHJ./JDDDDCIDCCELFEJ./DGDDDBEEDDAIJEJL/BDDDGHFHCBFBABBL/CLLLLCBCBBBBBBDB/DDDDDDDDDDDDLLLD/LFFFGEFGEHHFCBBL/LCCCICCDDHFCHFFL/LCECEDDDEDDCCHEL/LEECFFFFFFFFEFFL/.FFF.......IFFJ."),
        "tree" to sprite(".......FL......./.......CE......./.....FCEHEEJ..../.....LHFFIJ...../...JCCIFEKIJL.../...L.ICIEEEK..../..KFEFJHIIFKI.../...IKFDEKECFDHJ./..JCEICFLJHEH.../.JKIJLKHFEFJICI./..JFEEECEECEHIL./LHIJEEJFIHFHECF./..FJIHJKIJFEILL./EDEHKEFGGHEKIGD./EDFFGHIKDGIFEGE./....DDEEEDDCD..."),
        "bush" to sprite("........LFDCD.../......JFG...KFI./....LEI....ECACI/.....CJ.....IEH./.....CJ........./.....CJ........./.....CJ........./.....DI........./.....CI........./.....CI........./....KEH........./....LDFH......../...ELEGFE......./..EKLDJJDD....../.DJEFFBBFEDE..../...EDEEECE......"),
        "rubble" to sprite("................/.....BL........./....BBF........./...HBBF........./..KLBBFH.K.L..../...BBBFJFCLL..../...BCBFKCBBC..../.IGCCCKBBBBFL.G./..CHCBLEABFFFE../.FBBDHBCEFFFFHK./.EBELBBFKEBBBCE./.DBFLFEFICBBBFDE/ECFBBALBDIABFEJ./EDEEEFFEDBEEDBC./...EEFCEHCDED.../....E.D.ED......"),
        "streetlamp" to sprite(".......L.L....../.....L.FDLL...../.....IFKDEJD..../..HKKDBCIKCEL.../..KCHIJFCEIJ..../..LIECHHKKFCJIK./.ECFCDJJCJDEECJ./.KHIDFCJCJFDFEHL/.CDHJECJJHCCJFJ./JFDJICICHIEHIHI./KKIFCCIGJIIJHFJ./..FKEJIHICCKEIF./..JKIHJDHHHHJJEE/.FEEFJHJKJIHEDE./...EEFEIFEEDDE../......E..DD....."),
        "ceiling_lamp" to sprite("................/................/................/.......JJ......./......JAAJ....../.....JABBAJ...../....JABBBBAJ..../....JABBBBAJ..../.....JABBAJ...../......JAAJ....../.......JJ......./................/................/................/................/................"),
        "infected_shambler" to sprite(
            "....NNNN..../...NMMMMN.../...NMQMMN.../...NMMMMN.../....MMMM..../...NOOON..../..NOOOOON.../.NOOPPOOON../NOOOPPPOOON./NOOOPPOOOON./.NOOOOON..../..NOOON...../...OOON...../...OOON...../..NOOON...../..NOOON...../..NN.NN...../..NN.NN...../..NN.NN...../..NN.NN...../..NN..NN..../..NN..NN..../..NN..NN..../..NN..NN....",
            "....NNNN..../...NMMMMN.../...NMQMMN.../...NMMMMN.../....MMMM..../...NOOON..../..NOOOOON.../.NOOPPOOON../NOOOPPPOOON./NOOOPPOOOON./.NOOOOON..../..NOOON...../...OOON...../...OOON...../...OOON...../..NOOON...../..NN..NN..../..NN..NN..../..NN..NN..../..NN..NN..../..NN..NN..../..NN..NN..../..NN..NN..../..NN..NN...."
        ),
        "infected_runner" to sprite(
            "....NNNN..../...NMMMMN.../...NMQMMN.../...NMMMMN.../....MMMM..../..MMOOOO..../.MMOOOOOO.../MMOOPPOOOON./..OOPPOOOON./...OOOON..../...OOON...../..OOON....../..OOON....../..OOON....../..OOON....../..OOON....../...NN.NN..../...NN.NN..../...NN.NN..../..NN...NN.../..NN...NN.../..NN...NN.../.NN.....NN../.NN.....NN..",
            "....NNNN..../...NMMMMN.../...NMQMMN.../...NMMMMN.../....MMMM..../....OOOO..../..OOOOOO..../.NOOOOPPOON./NOOOOPPOOON./...OOOON..../....OOON..../....OOON..../...OOON...../...OOON...../...OOON...../...OOON...../..NN...NN.../..NN...NN.../..NN...NN.../..NN...NN.../...NN..NN.../...NN..NN.../....NN.NN.../....NN.NN..."
        ),
        "infected_brute" to sprite(
            "...NNNNNN.../..NMMMMMMN../..NMQMMMMN../..NMMMMMMN../...MMMMMM.../.NOOOOOOOON./NOOOOOOOOOON/NOOPPPPOOOON/NOOPPPPOOOON/NOOOOOOOOOON/.NOOOOOOOON./..NOOOOOON../...OOOOOO.../...OOOOOO.../...OOOOOO.../...OOOOOO.../..NNN..NNN../..NNN..NNN../..NNN..NNN../..NNN..NNN../..NNN..NNN../..NN...NN.../..NN...NN.../..NN...NN...",
            "...NNNNNN.../..NMMMMMMN../..NMQMMMMN../..NMMMMMMN../...MMMMMM.../.NOOOOOOOON./NOOOOOOOOOON/NOOPPPPOOOON/NOOPPPPOOOON/NOOOOOOOOOON/.NOOOOOOOON./..NOOOOOON../...OOOOOO.../...OOOOOO.../...OOOOOO.../...OOOOOO.../..NN..NNN.../..NN..NNN.../..NN..NNN.../..NN..NNN.../..NN..NNN.../..NN...NN.../..NN...NN.../..NN...NN..."
        )
    )

    /**
     * Redraw pass for the four player directions. The 16x24 blockout is expanded
     * to a 32x48 authored canvas, then individual texels are painted as seams,
     * webbing, pocket edges and small equipment highlights. Runtime atlas still
     * applies its separate 2x nearest-neighbour density pass.
     */
    private fun detailedPlayer(source: PixelSprite, direction: String): PixelSprite {
        val detailedFrames = source.frames.mapIndexed { frameIndex, frame ->
            val rows = buildList {
                frame.forEach { row ->
                    val doubled = buildString(row.length * 2) {
                        row.forEach { pixel -> append(pixel).append(pixel) }
                    }
                    add(doubled.toCharArray())
                    add(doubled.toCharArray())
                }
            }.toMutableList()

            fun paint(x: Int, y: Int, color: Char) {
                if (y in rows.indices && x in rows[y].indices && rows[y][x] != '.') {
                    rows[y][x] = color
                }
            }

            // Fine cloth seams and edge highlights, only on existing opaque pixels.
            for (y in 25..35) {
                paint(if (direction == "left") 11 else 20, y, 'X')
                if (y % 3 == 0) paint(if (direction == "left") 12 else 21, y, 'T')
            }
            when (direction) {
                "front" -> {
                    for (x in 21..25) paint(x, 28, 'T')
                    for (y in 28..31) paint(24, y, 'X')
                    paint(20, 30, 'V')
                }
                "back" -> {
                    for (y in 20..33) {
                        paint(12, y, 'U')
                        paint(13, y, 'T')
                        paint(24, y, 'U')
                    }
                    for (x in 13..22) paint(x, 31, 'X')
                    paint(22, 29, 'V')
                }
                "left", "right" -> {
                    val equipmentX = if (direction == "left") 7 else 24
                    for (y in 24..31) {
                        paint(equipmentX, y, 'U')
                        paint(equipmentX + 1, y, 'W')
                    }
                    paint(equipmentX, 25 + (frameIndex % 2), 'V')
                }
            }
            // Distinct boot/sole highlights make the walk cycle read at phone scale.
            for (x in 9..22) {
                if ((x + frameIndex) % 4 == 0) paint(x, 44, 'W')
            }
            rows.map { String(it) }
        }
        return PixelSprite(detailedFrames, palette)
    }

    private val detailedPlayerSprites = mapOf(
        "player" to detailedPlayer(base.getValue("player"), "front"),
        "player_back" to detailedPlayer(base.getValue("player_back"), "back"),
        "player_side_left" to detailedPlayer(base.getValue("player_side_left"), "left"),
        "player_side_right" to detailedPlayer(base.getValue("player_side_right"), "right")
    )

    /**
     * Fine-detail pass for major bunker fixtures. The original silhouette is
     * retained while the 32x32 canvas gains individually authored control
     * lights, panel seams, shelf/book edges and work-surface hardware.
     */
    private fun detailedBunkerProp(source: PixelSprite, id: String): PixelSprite {
        val frames = source.frames.map { frame ->
            val rows = buildList {
                frame.forEach { row ->
                    val doubled = buildString(row.length * 2) {
                        row.forEach { pixel -> append(pixel).append(pixel) }
                    }
                    add(doubled.toCharArray())
                    add(doubled.toCharArray())
                }
            }.toMutableList()
            fun paint(x: Int, y: Int, color: Char) {
                if (y in rows.indices && x in rows[y].indices && rows[y][x] != '.') rows[y][x] = color
            }
            when (id) {
                "bed" -> {
                    for (x in 8..23 step 2) paint(x, 12, 'T')
                    for (x in 8..23 step 3) paint(x, 24, 'X')
                    paint(9, 7, 'A'); paint(10, 7, 'T'); paint(22, 7, 'A')
                }
                "workbench" -> {
                    for (x in 7..25 step 3) paint(x, 7, 'W')
                    for (y in 12..25 step 3) paint(9, y, 'X')
                    paint(23, 8, 'V'); paint(24, 8, 'T')
                }
                "storage" -> {
                    for (y in 9..23 step 3) paint(7, y, 'T')
                    for (y in 9..23 step 3) paint(24, y, 'X')
                    paint(22, 16, 'V'); paint(23, 16, 'W')
                }
                "bookshelf" -> {
                    for (y in listOf(7, 19)) {
                        for (x in 7..24 step 3) paint(x, y, 'T')
                    }
                    for (x in listOf(9, 15, 21)) paint(x, 12, 'V')
                    for (x in listOf(11, 18, 24)) paint(x, 25, 'W')
                }
                "radio" -> {
                    for (x in 10..22 step 2) paint(x, 12, 'W')
                    paint(13, 15, 'V'); paint(14, 15, 'T')
                    paint(22, 22, 'A'); paint(24, 22, 'A')
                }
                "generator" -> {
                    for (y in 14..24 step 3) paint(8, y, 'X')
                    for (y in 14..24 step 3) paint(10, y, 'T')
                    paint(23, 11, 'V'); paint(24, 11, 'A')
                    for (x in 14..23 step 3) paint(x, 26, 'W')
                }
                "medical_station" -> {
                    for (x in 12..22 step 2) paint(x, 8, 'W')
                    paint(22, 13, 'V'); paint(23, 13, 'T')
                    for (x in 10..24 step 3) paint(x, 25, 'X')
                }
            }
            rows.map { String(it) }
        }
        return PixelSprite(frames, source.palette)
    }

    private val detailedBunkerProps = listOf(
        "bed", "workbench", "storage", "bookshelf", "radio", "generator", "medical_station"
    ).associateWith { id -> detailedBunkerProp(base.getValue(id), id) }

    val all: Map<String, PixelSprite> = base + detailedPlayerSprites + detailedBunkerProps + mapOf(
        "player_side" to base.getValue("player_side_right"),
        "storage" to base.getValue("locker"),
        "radio" to base.getValue("control_console"),
        "bunker_panel" to base.getValue("vault_door")
    )
}
