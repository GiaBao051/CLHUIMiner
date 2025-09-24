package CLHUIMiner;

import java.util.*;

public class TaxonomyNode {
    public String itemName;
    public int level;  // Cấp độ của item trong taxonomy
    public List<TaxonomyNode> children;

    public TaxonomyNode(String itemName, int level) {
        this.itemName = itemName;
        this.level = level;
        this.children = new ArrayList<>();
    }

    // Thêm child vào node
    public void addChild(TaxonomyNode child) {
        children.add(child);
    }
}
