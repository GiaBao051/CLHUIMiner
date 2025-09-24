package CLHUIMiner;

public class TaxonomyTree {
    private TaxonomyNode root;

    public TaxonomyTree() {
        root = new TaxonomyNode("root", 0);
    }

    // Thêm item vào taxonomy dưới một parent cụ thể
    public void addItem(String itemName, int level, String parentName) {
        TaxonomyNode parent = findNode(root, parentName);
        if (parent != null) {
            parent.addChild(new TaxonomyNode(itemName, level));
        }
    }

    // Tìm kiếm node trong taxonomy
    private TaxonomyNode findNode(TaxonomyNode node, String itemName) {
        if (node.itemName.equals(itemName)) {
            return node;
        }
        for (TaxonomyNode child : node.children) {
            TaxonomyNode found = findNode(child, itemName);
            if (found != null) return found;
        }
        return null;
    }

    // Lấy cấp độ của item
    public int getItemLevel(String itemName) {
        TaxonomyNode node = findNode(root, itemName);
        return (node != null) ? node.level : -1; 
    }
}

