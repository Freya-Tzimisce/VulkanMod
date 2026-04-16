package net.vulkanmod.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

public class CubeModel {

    private ModelPart.Polygon[] polygons = new ModelPart.Polygon[6];
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    Vector3f[] vertices;
    Vector3f[] transformed = new Vector3f[8];

    public void setVertices(int i, int j, float minX, float minY, float minZ, float dimX, float dimY, float dimZ, float growX, float growY, float growZ, boolean mirror, float uTexScale, float vTexScale, Set<Direction> set) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = minX + dimX;
        this.maxY = minY + dimY;
        this.maxZ = minZ + dimZ;
        this.polygons = new ModelPart.Polygon[set.size()];
        float s = maxX;
        float t = maxY;
        float u = maxZ;
        minX -= growX;
        minY -= growY;
        minZ -= growZ;
        s += growX;
        t += growY;
        u += growZ;
        if (mirror) {
            float v = s;
            s = minX;
            minX = v;
        }

        this.vertices = new Vector3f[]{
                new Vector3f(minX, minY, minZ),
                new Vector3f(s, minY, minZ),
                new Vector3f(s, t, minZ),
                new Vector3f(minX, t, minZ),
                new Vector3f(minX, minY, u),
                new Vector3f(s, minY, u),
                new Vector3f(s, t, u),
                new Vector3f(minX, t, u)
        };

        for (int i1 = 0; i1 < 8; i1++) {
            //pre-divide all vertices once
            this.vertices[i1].div(16.0f);
            this.transformed[i1] = new Vector3f(0.0f);
        }

        ModelPart.Vertex vertex1 = new ModelPart.Vertex(transformed[0], 0.0F, 0.0F);
        ModelPart.Vertex vertex2 = new ModelPart.Vertex(transformed[1], 0.0F, 8.0F);
        ModelPart.Vertex vertex3 = new ModelPart.Vertex(transformed[2], 8.0F, 8.0F);
        ModelPart.Vertex vertex4 = new ModelPart.Vertex(transformed[3], 8.0F, 0.0F);
        ModelPart.Vertex vertex5 = new ModelPart.Vertex(transformed[4], 0.0F, 0.0F);
        ModelPart.Vertex vertex6 = new ModelPart.Vertex(transformed[5], 0.0F, 8.0F);
        ModelPart.Vertex vertex7 = new ModelPart.Vertex(transformed[6], 8.0F, 8.0F);
        ModelPart.Vertex vertex8 = new ModelPart.Vertex(transformed[7], 8.0F, 0.0F);

        float w = (float)i;
        float x = (float)i + dimZ;
        float y = (float)i + dimZ + dimX;
        float z = (float)i + dimZ + dimX + dimX;
        float aa = (float)i + dimZ + dimX + dimZ;
        float ab = (float)i + dimZ + dimX + dimZ + dimX;
        float ac = (float)j;
        float ad = (float)j + dimZ;
        float ae = (float)j + dimZ + dimY;
        int idx = 0;
        if (set.contains(Direction.DOWN)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex6, vertex5, vertex1, vertex2}, x, ac, y, ad, uTexScale, vTexScale, mirror, Direction.DOWN);
        }

        if (set.contains(Direction.UP)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex3, vertex4, vertex8, vertex7}, y, ad, z, ac, uTexScale, vTexScale, mirror, Direction.UP);
        }

        if (set.contains(Direction.WEST)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex1, vertex5, vertex8, vertex4}, w, ad, x, ae, uTexScale, vTexScale, mirror, Direction.WEST);
        }

        if (set.contains(Direction.NORTH)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex2, vertex1, vertex4, vertex3}, x, ad, y, ae, uTexScale, vTexScale, mirror, Direction.NORTH);
        }

        if (set.contains(Direction.EAST)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex6, vertex2, vertex3, vertex7}, y, ad, aa, ae, uTexScale, vTexScale, mirror, Direction.EAST);
        }

        if (set.contains(Direction.SOUTH)) {
            this.polygons[idx] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex5, vertex6, vertex7, vertex8}, aa, ad, ab, ae, uTexScale, vTexScale, mirror, Direction.SOUTH);
        }
    }

    public void transformVertices(Matrix4f matrix) {
        // Transform original vertices and store them
        for (int i = 0; i < 8; ++i) {
            this.vertices[i].mulPosition(matrix, this.transformed[i]);
        }
    }

    public ModelPart.Polygon[] getPolygons() { return this.polygons; }
}
