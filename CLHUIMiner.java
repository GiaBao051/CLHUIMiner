package CLHUIMiner;

import java.io.*;
import java.util.*;

public class CLHUIMiner {

    long startTime = 0;
    long endTime = 0;
    int chuiCount = 0;
    int candidateCount = 0;
    Map<Integer, Integer> itemUtilityMap;
    BufferedWriter writer = null;
    int minUtility = 0;
    Map<Integer, Map<Integer, Integer>> itemPairUtilityMap;
    List<List<Itemset>> listItemsetsBySize = null;
    Set<Integer> setOfItemsInClosedItemsets = null;

    private TaxonomyTree taxonomyTree;

    public CLHUIMiner() {
        this.taxonomyTree = new TaxonomyTree(); 
    }

    public void addTaxonomyItem(String itemName, int level, String parentName) {
        taxonomyTree.addItem(itemName, level, parentName);
    }

    // Sắp xếp itemsets theo cấp độ taxonomy
    public void sortItemsetsByTaxonomy(List<UtilityList> listOfUtilityLists) {
        Collections.sort(listOfUtilityLists, new Comparator<UtilityList>() {
            public int compare(UtilityList o1, UtilityList o2) {
                // Chuyển đổi từ Integer sang String để lấy tên item
                String item1 = String.valueOf(o1.item);
                String item2 = String.valueOf(o2.item);
                
                // Lấy cấp độ của item từ taxonomy
                int level1 = taxonomyTree.getItemLevel(item1);
                int level2 = taxonomyTree.getItemLevel(item2);
                
                return Integer.compare(level1, level2);
            }
        });
    }

    public void runAlgorithm(String input, String output, int minUtility) throws IOException {
        MemoryLogger.getInstance().reset();
        this.minUtility = minUtility;
        if (output != null)
            writer = new BufferedWriter(new FileWriter(output));
        else {
            listItemsetsBySize = new ArrayList<List<Itemset>>();
            setOfItemsInClosedItemsets = new HashSet<Integer>();
        }

        startTime = System.currentTimeMillis();
        itemUtilityMap = new HashMap<Integer, Integer>();
        BufferedReader myInput = null;
        String thisLine;

        // Duyệt cơ sở dữ liệu lần 1 để tính TWU
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            while ((thisLine = myInput.readLine()) != null) {
                if (thisLine.isEmpty() || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }
                String split[] = thisLine.split(":");
                String items[] = split[0].split(" ");
                int transactionUtility = Integer.parseInt(split[1]);
                for (int i = 0; i < items.length; i++) {
                    Integer item = Integer.parseInt(items[i]);
                    Integer twu = itemUtilityMap.get(item);
                    if (twu == null)
                        twu = transactionUtility;
                    else
                        twu = twu + transactionUtility;
                    itemUtilityMap.put(item, twu);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }

        // Tạo danh sách các utility-list cho các item mở rộng
        List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();
        Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>();
        for (Integer item : itemUtilityMap.keySet()) {
            if (itemUtilityMap.get(item) >= minUtility) {
                UtilityList uList = new UtilityList(item);
                mapItemToUtilityList.put(item, uList);
                listOfUtilityLists.add(uList);
            }
        }

        // Sắp xếp các utility-list theo thứ tự TWU tăng dần
        sortItemsetsByTaxonomy(listOfUtilityLists);

        // Duyệt cơ sở dữ liệu lần 2 để xây dựng các utility-list
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            int tid = 0;
            while ((thisLine = myInput.readLine()) != null) {
                if (thisLine.isEmpty() || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }
                String split[] = thisLine.split(":");
                String items[] = split[0].split(" ");
                String utilityValues[] = split[2].split(" ");
                int newTU = 0;
                List<PairItemUtility> revisedTransaction = new ArrayList<PairItemUtility>();
                for (int i = 0; i < items.length; i++) {
                    PairItemUtility pair = new PairItemUtility();
                    pair.item = Integer.parseInt(items[i]);
                    pair.utility = Integer.parseInt(utilityValues[i]);
                    if (itemUtilityMap.get(pair.item) >= minUtility) {
                        revisedTransaction.add(pair);
                        newTU += pair.utility;
                    }
                }
                Collections.sort(revisedTransaction, new Comparator<PairItemUtility>() {
                    public int compare(PairItemUtility o1, PairItemUtility o2) {
                        return compareItems(o1.item, o2.item);
                    }
                });
                int remainingUtility = newTU;
                for (int i = 0; i < revisedTransaction.size(); i++) {
                    PairItemUtility pair = revisedTransaction.get(i);
                    remainingUtility = remainingUtility - pair.utility;
                    UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);
                    Element element = new Element(tid, pair.utility, remainingUtility);
                    utilityListOfItem.addElement(element);
                }
                tid++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }

        MemoryLogger.getInstance().checkMemory();
        findClosedItemsets(true, new int[0], null, new ArrayList<UtilityList>(), listOfUtilityLists);
        MemoryLogger.getInstance().checkMemory();
        if (writer != null)
            writer.close();
        endTime = System.currentTimeMillis();
    }

    // Hàm đệ quy chính để khai thác
    private void findClosedItemsets(boolean firstTime, int[] closedSet, UtilityList closedSetUL, List<UtilityList> preset, List<UtilityList> postset) throws IOException {

        for (UtilityList iUL : postset) {
            UtilityList newItemset_TIDs;
            if (firstTime)
                newItemset_TIDs = iUL;
            else
                newItemset_TIDs = construct(closedSetUL, iUL);

            // Kiểm tra xem itemset con có phải là HUI tiềm năng hay không
            if (isValidHUI(newItemset_TIDs)) {
                int[] newItemset = appendItem(closedSet, iUL.item);
                if (!isDuplicateUtilityList(newItemset_TIDs, preset)) {
                    int[] closedSetNew = newItemset;
                    UtilityList closedsetNewTIDs = newItemset_TIDs;
                    List<UtilityList> postsetNew = new ArrayList<UtilityList>();
                    boolean passedHUIPruning = true;

                    for (UtilityList jUL : postset) {
                        if (jUL.item == iUL.item || compareItems(jUL.item, iUL.item) < 0)
                            continue;
                        candidateCount++;
                        if (containsAllTIDS(jUL, newItemset_TIDs)) {
                            closedSetNew = appendItem(closedSetNew, jUL.item);
                            closedsetNewTIDs = construct(closedsetNewTIDs, jUL);
                            if (!isValidHUI(closedsetNewTIDs)) {
                                passedHUIPruning = false;
                                break;
                            }
                        } else {
                            postsetNew.add(jUL);
                        }
                    }
                    if (passedHUIPruning) {
                        if (closedsetNewTIDs.sumIutils >= minUtility) {
                            writeOut(closedSetNew, closedsetNewTIDs.sumIutils, closedsetNewTIDs.elements.size());
                        }
                        List<UtilityList> presetNew = new ArrayList<UtilityList>(preset);
                        findClosedItemsets(false, closedSetNew, closedsetNewTIDs, presetNew, postsetNew);
                    }
                    preset.add(iUL);
                }
            }
        }
    }

    // Kiểm tra xem tiện ích của itemset có hợp lệ hay không
    private boolean isValidHUI(UtilityList utilitylist) {
        return utilitylist.sumIutils + utilitylist.sumRutils >= minUtility;
    }

    // Kiểm tra xem utility-list có chứa tất cả các TID của utility-list khác không
    private boolean containsAllTIDS(UtilityList ul1, UtilityList ul2) {
        for (Element elmX : ul2.elements) {
            Element elmE = findElementWithTID(ul1, elmX.tid);
            if (elmE == null)
                return false;
        }
        return true;
    }

    private int[] appendItem(int[] itemset, int item) {
        int[] newItemset = new int[itemset.length + 1];
        System.arraycopy(itemset, 0, newItemset, 0, itemset.length);
        newItemset[itemset.length] = item;
        return newItemset;
    }

    // Kiểm tra trùng utility-list
    private boolean isDuplicateUtilityList(UtilityList newItemsetTIDs, List<UtilityList> preset) {
        for (UtilityList j : preset) {
            boolean containsAll = true;
            for (Element elmX : newItemsetTIDs.elements) {
                Element elmE = findElementWithTID(j, elmX.tid);
                if (elmE == null) {
                    containsAll = false;
                    break;
                }
            }
            if (containsAll)
                return true;
        }
        return false;
    }

    // Kết hợp hai utility-list
    private UtilityList construct(UtilityList uX, UtilityList uE) {
        UtilityList uXE = new UtilityList(uE.item);
        for (Element elmX : uX.elements) {
            Element elmE = findElementWithTID(uE, elmX.tid);
            if (elmE == null)
                continue;
            Element elmXe = new Element(elmX.tid, elmX.iutils + elmE.iutils, elmX.rutils - elmE.iutils);
            uXE.addElement(elmXe);
        }
        return uXE;
    }

    private int compareItems(int item1, int item2) {
        int compare = itemUtilityMap.get(item1) - itemUtilityMap.get(item2);
        return (compare == 0) ? item1 - item2 : compare;
    }

    private Element findElementWithTID(UtilityList ulist, int tid) {
        List<Element> list = ulist.elements;
        int first = 0;
        int last = list.size() - 1;
        while (first <= last) {
            int middle = first + (last - first) / 2;
            if (list.get(middle).tid < tid)
                first = middle + 1;
            else if (list.get(middle).tid > tid)
                last = middle - 1;
            else
                return list.get(middle);
        }
        return null;
    }

    private void writeOut(int[] itemset, long sumIutils, int support) throws IOException {
        chuiCount++;
        if (writer != null) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < itemset.length; i++) {
                buffer.append(itemset[i]);
                buffer.append(' ');
            }
            buffer.append(" #UTIL: ");
            buffer.append(sumIutils);
            writer.write(buffer.toString());
            writer.newLine();
        }
    }

    public void printStats() {
        System.out.println("=============  CLHUIMiner ALGORITHM  =============\n");
        System.out.println(" Total time ~ " + (endTime - startTime) + " ms");
        System.out.println(" Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println(" Closed High-utility itemsets count : " + chuiCount);
        System.out.println(" Candidate itemsets count : " + candidateCount);
        System.out.println("=====================================================");
    }
}
