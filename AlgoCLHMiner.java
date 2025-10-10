package CLHMiner;

import java.io.*;
import java.util.*;

public class AlgoCLHMiner {
    int minUtil;

    List<UtilityList> lstULs;

    int itemCount = 0;

    // Số lượng sản phẩm tổng quát (item có con)
    int giCount = 0;

    // Độ sâu lớn nhất của cây phân cấp
    int taxDepth = 0;

    static Map<Integer, UtilityList> mapItemToUtilityList;

    long startTimestamp = 0;

    Map<Integer, Double> mapItemToTWU;

    long endTimestamp = 0;

    TaxonomyTree taxonomy;

    private int[] itemsetBuffer = null;

    List<Pair> revisedTransaction;

    List<List<Pair>> datasetAfterRemove;

    int countHUI;

    int candidate;

    BufferedWriter writer;

    class Pair {
        int item = 0;
        double utility = 0;
    }

    public void runAlgorithm(int minUtil, String input, String outputPath, String Taxonomy) throws IOException {
        writer = new BufferedWriter(new FileWriter(outputPath)); // Mở file để ghi kết quả
        this.minUtil = minUtil; // Gán ngưỡng tiện ích
        candidate = 0; // Reset bộ đếm ứng cử viên
        startTimestamp = System.currentTimeMillis(); // Ghi lại thời gian bắt đầu
        mapItemToTWU = new HashMap<>(); // Khởi tạo map chứa TWU
        taxonomy = new TaxonomyTree(); // Khởi tạo cây phân cấp
        taxonomy.ReadDataFromPath(Taxonomy); // Đọc và xây dựng cây từ file
        BufferedReader myInput = null;
        itemsetBuffer = new int[500]; // Khởi tạo bộ đệm itemset
        datasetAfterRemove = new ArrayList<>(); // Khởi tạo CSDL sau khi lọc
        countHUI = 0;
        Set<Integer> itemInDB = new HashSet<>(); // Lưu các item duy nhất trong DB
        String thisLine;

        // QUÉT CƠ SỞ DỮ LIỆU LẦN ĐẦU ĐỂ TÍNH TWU

        myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
        // Duyệt qua từng dòng (từng giao dịch) trong file
        while ((thisLine = myInput.readLine()) != null) {
            // Bỏ qua các dòng trống hoặc dòng ghi chú
            if (thisLine.isEmpty() || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                    || thisLine.charAt(0) == '@') {
                continue;
            }

            // Tách dòng giao dịch bằng dấu hai chấm ":"
            String[] split = thisLine.split(":");
            // Phần đầu tiên là danh sách các sản phẩm
            String[] items = split[0].split(" ");
            // Phần thứ hai là tổng tiện ích của giao dịch (Transaction Utility)
            double transactionUtility = Double.parseDouble(split[1]);
            // Dùng Set để lưu các "cha" của các item trong giao dịch này, tránh lặp lại
            HashSet<Integer> setParent = new HashSet<>();

            for (int i = 0; i < items.length; i++) {
                Integer item = Integer.parseInt(items[i]);
                itemInDB.add(item);

                // Nếu item chưa tồn tại trong cây phân cấp, thêm nó vào như một nút gốc
                if (taxonomy.mapItemToTaxonomyNode.get(item) == null) {
                    TaxonomyNode newNode = new TaxonomyNode(item);
                    taxonomy.mapItemToTaxonomyNode.get(-1).addChildren(newNode);
                    taxonomy.mapItemToTaxonomyNode.put(item, newNode);
                } else {
                    // Nếu item đã có, tìm tất cả các tổ tiên (cha, ông,...) của nó
                    TaxonomyNode parentNode = taxonomy.mapItemToTaxonomyNode.get(item).getParent();
                    while (parentNode.getData() != -1) { // Lặp cho đến khi gặp gốc ảo (-1)
                        setParent.add(parentNode.getData()); // Thêm tổ tiên vào Set
                        parentNode = parentNode.getParent();
                    }
                }

                // Cập nhật TWU cho sản phẩm hiện tại
                Double twu = mapItemToTWU.get(item);
                if (twu == null) {
                    // Nếu chưa có (đây là lần đầu gặp sản phẩm này)
                    // gán TWU của nó bằng tiện ích của giao dịch hiện tại
                    twu = transactionUtility;
                } else {
                    // Nếu đã có giá trị (sản phẩm này đã xuất hiện ở giao dịch trước đó)
                    // cộng dồn tiện ích của giao dịch hiện tại vào giá trị TWU cũ
                    twu = twu + transactionUtility;
                }
                mapItemToTWU.put(item, twu);
            }
            // Cập nhật TWU cho tất cả các "cha" (tổ tiên) của các sản phẩm trong giao dịch
            for (Integer parentItemInTransaction : setParent) {
                Double twu = mapItemToTWU.get(parentItemInTransaction);
                if (twu == null) {
                    twu = transactionUtility;
                } 
                else {
                    twu = twu + transactionUtility;
                }
                mapItemToTWU.put(parentItemInTransaction, twu);
            }
        }

        //LỌC CÁC SẢN PHẨM KHÔNG TIỀM NĂNG VÀ SẮP XẾP
        
        List<UtilityList> listOfUtilityLists = new ArrayList<>();
        mapItemToUtilityList = new HashMap<>();

        // Duyệt qua tất cả các sản phẩm đã tính TWU
        for (Integer item : mapItemToTWU.keySet()) {
            // Nếu sản phẩm có tiềm năng (TWU >= ngưỡng tối thiểu)
            if (mapItemToTWU.get(item) >= minUtil) {
                UtilityList uList = new UtilityList(item);
                mapItemToUtilityList.put(item, uList);
                // Thêm vào danh sách các sản phẩm tiềm năng để xử lý tiếp
                listOfUtilityLists.add(uList);
            }
        }

        // Sắp xếp danh sách các sản phẩm tiềm năng theo thứ tự tăng dần
        Collections.sort(listOfUtilityLists, new Comparator<UtilityList>() {
            public int compare(UtilityList o1, UtilityList o2) {
                return compareItems(o1.item, o2.item);
            }
        });

        //QUÉT CƠ SỞ DỮ LIỆU LẦN HAI ĐỂ XÂY DỰNG UTILITY LIST
        
        myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
        int tid = 0;
        while ((thisLine = myInput.readLine()) != null) {
            if (thisLine.isEmpty() || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@')
                continue;
            String[] split = thisLine.split(":");
            String[] items = split[0].split(" ");
            // Phần thứ ba là danh sách tiện ích của từng sản phẩm
            String[] utilityValues = split[2].split(" ");
            // Xây dựng lại giao dịch chỉ với các sản phẩm tiềm năng
            double remainingUtility = 0; // Biến tính tiện ích còn lại
            double TU = Double.parseDouble(split[1]); // Tổng tiện ích giao dịch
            List<Pair> revisedTransaction = new ArrayList<>(); // Giao dịch đã được lọc

            HashMap<Integer, Double> mapParentToUtility = new HashMap<>();

            for (int i = 0; i < items.length; i++) {
                Double utility = Double.parseDouble(utilityValues[i]);
                int item = Integer.parseInt(items[i]);
                // Cộng dồn tiện ích của item này cho tất cả các tổ tiên của nó
                TaxonomyNode nodeParent = taxonomy.mapItemToTaxonomyNode.get(item).getParent();
                while (nodeParent.getData() != -1) {
                    Double utilityOfParent = mapParentToUtility.get(nodeParent.getData());
                    if (utilityOfParent != null) {
                        mapParentToUtility.put(nodeParent.getData(), utilityOfParent + utility);
                    } else {
                        mapParentToUtility.put(nodeParent.getData(), utility);
                    }
                    nodeParent = nodeParent.getParent();
                }
                // Nếu sản phẩm này tiềm năng (đã qua bước lọc TWU)
                if (mapItemToTWU.get(item) >= minUtil) {
                    Pair pair = new Pair();
                    pair.item = item;
                    pair.utility = utility;
                    revisedTransaction.add(pair);
                    remainingUtility += pair.utility; // Cộng dồn tiện ích cho remainingUtility
                }
            }

            // Sắp xếp lại giao dịch đã lọc theo thứ tự
            Collections.sort(revisedTransaction, new Comparator<Pair>() {
                public int compare(Pair o1, Pair o2) {
                    return compareItems(o1.item, o2.item);
                }
            });

            double CountUtility = remainingUtility;

            // Duyệt qua giao dịch đã lọc để tạo các phần tử (Element) cho UtilityList
            for (int i = 0; i < revisedTransaction.size(); i++) {
                Pair pair = revisedTransaction.get(i);
                // Tiện ích còn lại (rutils) là tổng tiện ích của các item đứng sau nó
                remainingUtility = remainingUtility - pair.utility;
                UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);
                // Tạo một phần tử mới với (tid, iutils, rutils, TU)
                Element element = new Element(tid, pair.utility, remainingUtility, TU);
                utilityListOfItem.addElement(element); // Thêm phần tử vào UtilityList của item
            }

            // Tạo các phần tử cho UtilityList của các sản phẩm "cha"
            for (Integer itemParent : mapParentToUtility.keySet()) {
                double CountUtilityOfEachItem = CountUtility;
                // Tính rutils cho sản phẩm cha
                for (int i = 0; i < revisedTransaction.size(); i++) {
                    Pair CurrentItem = revisedTransaction.get(i);
                    // Logic phức tạp để tính rutils cho item cha
                    if (CheckParent(itemParent, CurrentItem.item)) {
                        CountUtilityOfEachItem -= CurrentItem.utility;
                    } else {
                        if (compareItems(itemParent, CurrentItem.item) > 0) {
                            CountUtilityOfEachItem -= CurrentItem.utility;
                        }
                    }
                }
                UtilityList utilityListOfItem = mapItemToUtilityList.get(itemParent);
                if (utilityListOfItem != null) { // Chỉ xử lý nếu cha là item tiềm năng
                    Element element = new Element(tid, mapParentToUtility.get(itemParent), CountUtilityOfEachItem, TU);
                    utilityListOfItem.addElement(element);
                }
            }
            datasetAfterRemove.add(revisedTransaction); // Lưu lại giao dịch đã lọc
            tid++; // Tăng ID giao dịch cho lần lặp tiếp theo
        }

        // BẮT ĐẦU QUÁ TRÌNH KHAI PHÁ ĐỆ QUY
        // Lấy danh sách các sản phẩm ở cấp 1 (con trực tiếp của gốc ảo) để bắt đầu tìm kiếm
        List<UtilityList> listUtilityLevel1 = new ArrayList<>();
        for (UtilityList ul1 : listOfUtilityLists) {
            if (taxonomy.getMapItemToTaxonomyNode().get(ul1.item).getLevel() == 1) {
                listUtilityLevel1.add(ul1);
            }
            // Vì danh sách đã sắp xếp theo cấp độ, có thể dừng sớm
            if (taxonomy.getMapItemToTaxonomyNode().get(ul1.item).getLevel() > 1) {
                break;
            }
        }

        // Cập nhật các thông số thống kê
        itemCount = itemInDB.size();
        giCount = taxonomy.getGI() - 1;
        taxDepth = taxonomy.getMaxLevel();

        // Bắt đầu tìm kiếm đệ quy từ các sản phẩm cấp 1
        SearchTree(itemsetBuffer, 0, null, listUtilityLevel1);
        endTimestamp = System.currentTimeMillis(); // Ghi lại thời gian kết thúc
        myInput.close();
        writer.close();
    }

    private void SearchTree(int[] prefix, int prefixLength, UtilityList pUL, List<UtilityList> ULs) throws IOException {
        // Duyệt qua danh sách các ứng cử viên để mở rộng
        for (int i = 0; i < ULs.size(); i++) {
            UtilityList X = ULs.get(i);
            candidate++; // Tăng bộ đếm ứng cử viên

            // KIỂM TRA ĐIỀU KIỆN TIỆN ÍCH CAO
            if (X.sumIutils > minUtil) {
                countHUI++; // Tăng bộ đếm HUI
                for (int j = 0; j < prefixLength; j++) {
                    writer.write(prefix[j] + " ");
                }
                // Ghi sản phẩm cuối cùng và tiện ích của nó
                writer.write(X.item + " #UTIL: " + X.sumIutils);
                writer.newLine();
            }

            // MỞ RỘNG NGANG (SIBLING EXTENSION)
            /*Tạo ra các ứng cử viên mới bằng cách kết hợp X 
            với các mục "anh em" (cùng cấp, không phải cha-con) đứng sau nó */
            List<UtilityList> exULs = new ArrayList<>(); // Danh sách các phần mở rộng mới
            // Kết hợp X với các item Y đứng sau nó trong danh sách
            for (int j = i + 1; j < ULs.size(); j++) {
                UtilityList Y = ULs.get(j);
                // Chỉ kết hợp nếu X và Y không có quan hệ cha-con
                if (!CheckParent(Y.item, X.item)) {
                    // Xây dựng UtilityList cho tập hợp mới XY
                    UtilityList exULBuild = construct(pUL, X, Y);
                    // Nếu tập hợp mới có tiềm năng (GWU > minUtil) thì thêm vào danh sách mở rộng
                    if (exULBuild.GWU > minUtil) {
                        exULs.add(exULBuild);
                    }
                }
            }

            // MỞ RỘNG DỌC (CHILD EXTENSION) - CẮT TỈA
            /*Nếu X vẫn còn "tiềm năng" (tổng tiện ích thực tế + tiện ích còn lại > minUtil),
            nó sẽ tiếp tục tạo ứng cử viên bằng cách kết hợp X với
            các mục "con" của nó trong cây phân cấp. */
            // Nếu tổng tiện ích và tiện ích còn lại (cận trên) lớn hơn ngưỡng
            if (X.sumIutils + X.sumRutils > minUtil) {
                TaxonomyNode taxonomyNodeX = taxonomy.getMapItemToTaxonomyNode().get(X.item);
                List<TaxonomyNode> childOfX = taxonomyNodeX.getChildren();
                for (TaxonomyNode taxonomyNode : childOfX) {
                    int Child = taxonomyNode.getData();
                    UtilityList ULofChild = mapItemToUtilityList.get(Child);
                    if (ULofChild != null) { // Nếu con là item tiềm năng
                        // Xây dựng UtilityList cho tập hợp (Tiền tố + Con)
                        UtilityList exULBuild = constructTax(pUL, ULofChild);
                        X.AddChild(exULBuild); // Thêm vào danh sách con của X
                    }
                }
                // Thêm các con tiềm năng vào danh sách ứng cử viên hiện tại để xét
                for (UtilityList childULs : X.getChild()) {
                    if (childULs.GWU > minUtil) {
                        ULs.add(childULs);
                    }
                }
            }

            // ĐỆ QUY
            itemsetBuffer[prefixLength] = X.item; // Thêm X vào tiền tố
            // Gọi lại hàm tìm kiếm với tiền tố mới và danh sách mở rộng ngang
            SearchTree(itemsetBuffer, prefixLength + 1, X, exULs);
        }
    }

    private UtilityList constructTax(UtilityList P, UtilityList Child) {
        if (P == null) {
            return Child; // Nếu không có tiền tố, trả về chính UL của con
        } else {
            UtilityList newULs = new UtilityList(Child.item);
            for (Element PElment : P.getElement()) {
                // Tìm phần tử tương ứng trong UL của con (cùng tid)
                Element UnionChild = findElementWithTID(Child, PElment.tid);
                if (UnionChild != null) { // Nếu tìm thấy (tức là cả P và Child cùng xuất hiện)
                    List<Pair> trans = datasetAfterRemove.get(UnionChild.tid);
                    double remainUtility = 0;
                    // Tính rutils mới
                    for (int i = 0; i < trans.size(); i++) {
                        Integer currentItem = trans.get(i).item;
                        if (compareItems(currentItem, Child.item) > 0 && (!CheckParent(Child.item, currentItem))
                                && (!CheckParent(Child.item, currentItem))) {
                            remainUtility += trans.get(i).utility;
                        }
                    }

                    // Tạo phần tử mới: iutils = iutils(P) + iutils(Child)
                    Element newElment = new Element(UnionChild.tid, PElment.iutils + UnionChild.iutils, remainUtility, UnionChild.TU);
                    newULs.addElement(newElment); // Thêm vào UL mới
                }
            }
            return newULs;
        }
    }

    private UtilityList construct(UtilityList P, UtilityList px, UtilityList py) {
        UtilityList pxyUL = new UtilityList(py.item); // UL mới cho Pxy

        // Đối với mỗi phần tử trong danh sách tiện ích của px
        for (Element ex : px.elements) {
            // Tìm phần tử ey tương ứng trong py với cùng tid
            Element ey = findElementWithTID(py, ex.tid);
            if (ey == null) {
                continue; // Nếu không tìm thấy, bỏ qua
            }

            // Nếu tiền tố P là null (trường hợp tạo 2-itemset)
            if (P == null) {
                List<Pair> trans = datasetAfterRemove.get(ex.tid);
                double remainUtility = 0;
                // Tính rutils cho tập hợp mới xy
                for (int i = 0; i < trans.size(); i++) {
                    Integer currentItem = trans.get(i).item;
                    if (compareItems(currentItem, py.item) > 0 && (!CheckParent(px.item, currentItem))
                            && (!CheckParent(py.item, currentItem))) {
                        remainUtility += trans.get(i).utility;
                    }
                }
                // Tạo phần tử mới: iutils(xy) = iutils(x) + iutils(y)
                Element eXY = new Element(ex.tid, ex.iutils + ey.iutils, remainUtility, ey.TU);
                pxyUL.addElement(eXY);
            } else { // Nếu có tiền tố P
                // Tìm phần tử của P trong cùng giao dịch
                Element e = findElementWithTID(P, ex.tid);
                if (e != null) {
                    List<Pair> trans = datasetAfterRemove.get(e.tid);
                    double remainUtility = 0;
                    // Tính rutils cho tập hợp mới pxy
                    for (int i = 0; i < trans.size(); i++) {
                        Integer currentItem = trans.get(i).item;
                        if (compareItems(currentItem, py.item) > 0 && (!CheckParent(px.item, currentItem))
                                && (!CheckParent(py.item, currentItem))) {
                            remainUtility += trans.get(i).utility;
                        }
                    }
                    // Tạo phần tử mới: iutils(pxy) = iutils(px) + iutils(py) - iutils(p)
                    Element eXY = new Element(ex.tid, ex.iutils + ey.iutils - e.iutils, remainUtility, ey.TU);
                    pxyUL.addElement(eXY);
                }
            }
        }
        return pxyUL; // Trả về UtilityList mới
    }

    private Element findElementWithTID(UtilityList ulist, int tid) {
        List<Element> list = ulist.elements;
        int first = 0;
        int last = list.size() - 1;

        while (first <= last) {
            int middle = (first + last) >>> 1; // Tương đương (first + last) / 2
            if (list.get(middle).tid < tid) {
                first = middle + 1;
            } else if (list.get(middle).tid > tid) {
                last = middle - 1;
            } else {
                return list.get(middle); // Tìm thấy
            }
        }
        return null; // Không tìm thấy
    }

    private int compareItems(int item1, int item2) {
        int levelOfItem1 = taxonomy.getMapItemToTaxonomyNode().get(item1).getLevel();
        int levelOfItem2 = taxonomy.getMapItemToTaxonomyNode().get(item2).getLevel();
        if (levelOfItem1 == levelOfItem2) { // Nếu cùng cấp độ
            // So sánh dựa trên TWU
            int compare = (int) (mapItemToTWU.get(item1) - mapItemToTWU.get(item2));
            return (compare == 0) ? item1 - item2 : compare; // Nếu TWU bằng nhau thì so sánh ID
        } else {
            // So sánh dựa trên cấp độ
            return levelOfItem1 - levelOfItem2;
        }
    }

    private boolean CheckParent(int item1, int item2) {
        TaxonomyNode nodeItem1 = taxonomy.getMapItemToTaxonomyNode().get(item1);
        TaxonomyNode nodeItem2 = taxonomy.getMapItemToTaxonomyNode().get(item2);
        int levelOfItem1 = nodeItem1.getLevel();
        int levelOfItem2 = nodeItem2.getLevel();
        if (levelOfItem1 == levelOfItem2) {
            return false; // Cùng cấp độ không thể là cha-con
        } else {
            if (levelOfItem1 > levelOfItem2) { // item1 ở cấp độ sâu hơn -> có thể là con của item2
                TaxonomyNode parentItem1 = nodeItem1.getParent();
                while (parentItem1.getData() != -1) { // Đi ngược lên cây từ item1
                    if (parentItem1.getData() == nodeItem2.getData()) {
                        return true; // Tìm thấy item2 là tổ tiên của item1
                    }
                    parentItem1 = parentItem1.getParent();
                }
                return false;
            } else { // item2 ở cấp độ sâu hơn -> có thể là con của item1
                TaxonomyNode parentItem2 = nodeItem2.getParent();
                while (parentItem2.getData() != -1) { // Đi ngược lên cây từ item2
                    if (parentItem2.getData() == nodeItem1.getData()) {
                        return true; // Tìm thấy item1 là tổ tiên của item2
                    }
                    parentItem2 = parentItem2.getParent();
                }
                return false;
            }
        }
    }

    public void printStats() throws IOException {
        System.out.println("=============  CLH-Miner =============");
        System.out.println(" Runtime time ~ : " + (endTimestamp - startTimestamp) + " ms");
        System.out.println(" Memory ~ : " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println(" Cross level high utility itemsets (count): " + countHUI);
        System.out.println("   Number of items              : " + itemCount);
        System.out.println("   Number of generalized items             : " + giCount);
        System.out.println("   Taxonomy depth   : " + taxDepth);
        System.out.println("   Candidates (count): " + candidate);
        System.out.println("======================================");
    }
}