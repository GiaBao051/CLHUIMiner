package CLHMiner;

import java.io.*;
import java.util.*;

public class TaxonomyTree {

	private TaxonomyNode Root;
	private int GI;
	private int I;
	private int MaxLevel;

	HashMap<Integer,TaxonomyNode>mapItemToTaxonomyNode;

	public TaxonomyNode getRoot() {
		return Root;
	}

	public void setRoot(TaxonomyNode root) {
		Root = root;
	}

	public int getGI() {
		return GI;
	}

	public void setGI(int gI) {
		GI = gI;
	}

	public int getI() {
		return I;
	}

	public void setI(int i) {
		I = i;
	}

	public int getMaxLevel() {
		return MaxLevel;
	}

	public void setMaxLevel(int maxLevel) {
		MaxLevel = maxLevel;
	}

	public TaxonomyTree() {
		Root = new TaxonomyNode(-1);
		mapItemToTaxonomyNode = new HashMap<Integer, TaxonomyNode>();
		mapItemToTaxonomyNode.put(-1, Root);
		GI = 0;
		I = 0;
		MaxLevel = 0;
	}

	public void ReadDataFromPath(String Path) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Path))));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '@') {
					continue;
				}
				String tokens[] = line.split(",");
				Integer child = Integer.parseInt(tokens[0]);
				Integer parent = Integer.parseInt(tokens[1]);
				TaxonomyNode nodeParent = mapItemToTaxonomyNode.get(parent);
				if (nodeParent == null) {
					nodeParent = new TaxonomyNode(parent);
					TaxonomyNode nodeChildren = mapItemToTaxonomyNode.get(child);
					if (nodeChildren == null) {
						nodeChildren = new TaxonomyNode(child);
						nodeParent.addChildren(nodeChildren);
						mapItemToTaxonomyNode.put(child, nodeChildren);
					} else {
						nodeParent.addChildren(nodeChildren);
					}
					mapItemToTaxonomyNode.put(parent, nodeParent);
				} else {
					TaxonomyNode nodeChildren = mapItemToTaxonomyNode.get(child);
					if (nodeChildren == null) {
						nodeChildren = new TaxonomyNode(child);
						nodeParent.addChildren(nodeChildren);
						mapItemToTaxonomyNode.put(child, nodeChildren);
					} else {
						nodeParent.addChildren(nodeChildren);
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				reader.close();
			}
			for (Integer item : mapItemToTaxonomyNode.keySet()) {
				if (item != -1) {
					TaxonomyNode node = mapItemToTaxonomyNode.get(item);
					if (node.getParent() == null) {
						Root.addChildren(node);
					}
				}

			}
			SetLevelForNode();
		}
	}

	public void SetLevelForNode() {
		for (Integer item : mapItemToTaxonomyNode.keySet()) {
			int currentLevel = 0;
			if (item != -1) {
				currentLevel = 1;
				TaxonomyNode parent = mapItemToTaxonomyNode.get(item).getParent();
				while (parent.getData() != -1) {
					currentLevel++;
					parent = parent.getParent();
				}
			}
			if (mapItemToTaxonomyNode.get(item).getChildren().size() == 0) {
				I++;
			} else {
				GI++;
			}
			mapItemToTaxonomyNode.get(item).setLevel(currentLevel);
			if (currentLevel > MaxLevel) {
				MaxLevel = currentLevel;
			}

		}

	}

	public HashMap<Integer, TaxonomyNode> getMapItemToTaxonomyNode() {
		return mapItemToTaxonomyNode;
	}

	public void setMapItemToTaxonomyNode(HashMap<Integer, TaxonomyNode> mapItemToTaxonomyNode) {
		this.mapItemToTaxonomyNode = mapItemToTaxonomyNode;
	}
}
