/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package net.refractions.udig.project.internal.render;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.StyleContent;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.ProjectPlugin;
import net.refractions.udig.project.preferences.PreferenceConstants;
import net.refractions.udig.ui.graphics.SLDs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.ConstantExpression;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.function.FilterFunction_geometryType;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleAttributeExtractor;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.UserLayer;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class that manages the style used for Selection.
 * <p>
 * This class is in charge of making a good selection based on preferences configured by the user.
 * </p>
 * @author jeichar
 * @since 0.6.0
 */
public class SelectionStyleContent extends StyleContent {

    /** <code>ID</code> field */
    public static final String ID = "net.refractions.udig.project.selectionStyle"; //$NON-NLS-1$

    private static final StyleBuilder builder = new StyleBuilder();
    private static final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(GeoTools
            .getDefaultHints());

    private static final String fTypeName = "fTypeName"; //$NON-NLS-1$

    private static Color getSelectionColor() {
        IPreferenceStore store = ProjectPlugin.getPlugin().getPreferenceStore();
        RGB rgb = PreferenceConverter.getColor(store, PreferenceConstants.P_SELECTION_COLOR);
        return new Color(rgb.red, rgb.green, rgb.blue);
    }
    private static Color getSelectionColor2() {
        IPreferenceStore store = ProjectPlugin.getPlugin().getPreferenceStore();
        RGB rgb = PreferenceConverter.getColor(store, PreferenceConstants.P_SELECTION2_COLOR);
        return new Color(rgb.red, rgb.green, rgb.blue, 75);
    }

    private StyleBuilder styleBuilder = new StyleBuilder();

    /**
     * Construct <code>SelectionStyleContent</code>.
     */
    public SelectionStyleContent() {
        super(ID);
    }

    /**
     * Returns a geotools style object.
     */
    public Class getStyleClass() {
        return Style.class;
    }

    /**
     * Does nothing.
     */
    public void save( IMemento momento, Object value ) {
        Style style = (Style) value;

        // store the feature type name
        momento.putString(fTypeName, style.getFeatureTypeStyles()[0].getFeatureTypeName());
    }

    public Object load( IMemento momento ) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.refractions.udig.project.StyleContent#createDefaultStyle(net.refractions.udig.catalog.IGeoResource)
     */
    public Object createDefaultStyle( IGeoResource resource, Color colour, IProgressMonitor monitor ) {
        // this is purposely no implemented
        return null;
    }

    public static Style createDefaultStyle( Layer layer ) {
        SimpleFeatureType schema = layer.getSchema();
        return createDefaultStyle(schema);
    }

    /**
     *
     * @param schema
     * @return
     */
    private static Style createDefaultStyle( SimpleFeatureType schema ) {
        Style style = builder.createStyle();

        SelectionStyleContent ssc = new SelectionStyleContent();
        if (schema == null)
            return null;
        GeometryDescriptor geom = schema.getGeometryDescriptor();
        if (isLine(geom)) {
            ssc.getDefaultRule(style).setSymbolizers(createLineSymbolizers(ssc));
        } else if (isPoint(geom)) {
            int size = queryPointSize(style);
            PointSymbolizer sym = ssc.createPointSymbolizer(getSelectionColor(), size);
            Symbolizer[] symbolizers = new Symbolizer[]{sym};
            ssc.getDefaultRule(style).setSymbolizers(symbolizers);
        } else if (isPolygon(geom)) {
            ssc.getDefaultRule(style).setSymbolizers(createPolygonSymbolizarts(ssc));
        } else {
            try {
                int size = queryPointSize(style);
                ssc.createGeometrySLD(getSelectionColor2(), getSelectionColor(), schema
                        .getGeometryDescriptor().getName().getLocalPart(), style, size);
            } catch (Exception e) {
                ProjectPlugin.log("", e); //$NON-NLS-1$
                ssc.getDefaultRule(style).setSymbolizers(
                        new Symbolizer[]{ssc.createLineSymbolizer(getSelectionColor2(), false),
                                ssc.createLineSymbolizer(getSelectionColor(), true)});
            }
        }

        // nest the style in an SLD
        createDefaultStyledLayerDescriptor(style);
        return style;
    }
    
    private static int queryPointSize( Object style ) {
        int size = 6;
        if( style instanceof Style ){
            PointSymbolizer pointSymbolizer = SLD.pointSymbolizer( (Style) style );
            if( pointSymbolizer != null ){
                Mark mark = SLD.mark( pointSymbolizer );
                if( mark != null ){
                    size = SLD.size( mark );
                }
            }
        }
        if( style instanceof FeatureTypeStyle){
            PointSymbolizer pointSymbolizer = SLD.pointSymbolizer( (FeatureTypeStyle) style );
            if( pointSymbolizer != null ){
                Mark mark = SLD.mark( pointSymbolizer );
                if( mark != null ){
                    size = SLD.size( mark );
                }
            }
        }
        return size;
    }
    /**
     * @param ssc
     * @return
     */
    private static Symbolizer[] createLineSymbolizers( SelectionStyleContent ssc ) {
        return new Symbolizer[]{ssc.createLineSymbolizer(getSelectionColor2(), false, 3, 0.5),
                ssc.createLineSymbolizer(getSelectionColor(), true)};
    }
    /**
     * @param ssc
     * @return
     */
    private static Symbolizer[] createPolygonSymbolizarts( SelectionStyleContent ssc ) {
        return new Symbolizer[]{ssc.createPolygonSymbolizer(getSelectionColor(), true),
                ssc.createLineSymbolizer(getSelectionColor2(), false, 3, 0.5),
                ssc.createLineSymbolizer(getSelectionColor(), false, 1, 1),};
    }

    private PropertyIsEqualTo createGeometryFunctionFilter( String geomXPath,
            Object geometryClassSimpleName ) throws IllegalFilterException {
        FilterFactory factory = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        FilterFunction_geometryType geomTypeExpr = new FilterFunction_geometryType();
        List<Expression> params = Arrays.asList(new Expression[]{factory.property(geomXPath)});
        geomTypeExpr.setParameters(params);

        PropertyIsEqualTo filter = factory.equals(geomTypeExpr, factory
                .literal(geometryClassSimpleName));
        return filter;
    }

    private void createGeometrySLD( Color colour, Color colour2, String geomXPath, Style style,
            int size ) throws IllegalFilterException {
        // create Point rule
        Rule rule = getDefaultRule(style);
        PropertyIsEqualTo filter = createGeometryFunctionFilter(geomXPath, Point.class
                .getSimpleName());
        rule.setFilter(filter);
        Symbolizer[] pointSymbolizers = new Symbolizer[]{createPointSymbolizer(colour, size)};
        rule.setSymbolizers(pointSymbolizers);
        getDefaultFeatureTypeStyle(style).addRule(rule);

        // create MultiPoint rule
        rule = styleBuilder.createRule(new Symbolizer[]{});
        filter = createGeometryFunctionFilter(geomXPath, MultiPoint.class.getSimpleName());
        rule.setFilter(filter);
        rule.setSymbolizers(pointSymbolizers);
        getDefaultFeatureTypeStyle(style).addRule(rule);

        // create LineString rule
        rule = styleBuilder.createRule(new Symbolizer[]{});
        filter = createGeometryFunctionFilter(geomXPath, LineString.class.getSimpleName());
        rule.setFilter(filter);
        Symbolizer[] lineSymbolizers = createLineSymbolizers(this);
        rule.setSymbolizers(lineSymbolizers);
        getDefaultFeatureTypeStyle(style).addRule(rule);

        // create LinearRing rule
        rule = styleBuilder.createRule(new Symbolizer[]{});
        filter = createGeometryFunctionFilter(geomXPath, LinearRing.class.getSimpleName());
        rule.setFilter(filter);
        rule.setSymbolizers(lineSymbolizers);
        getDefaultFeatureTypeStyle(style).addRule(rule);

        // create MultiLineString rule
        rule = styleBuilder.createRule(new Symbolizer[]{});
        filter = createGeometryFunctionFilter(geomXPath, MultiLineString.class.getSimpleName());
        rule.setFilter(filter);
        rule.setSymbolizers(lineSymbolizers);
        getDefaultFeatureTypeStyle(style).addRule(rule);

        // create Polygon rule
        rule = styleBuilder.createRule(new Symbolizer[]{});
        filter = createGeometryFunctionFilter(geomXPath, Polygon.class.getSimpleName());
        rule.setFilter(filter);
        Symbolizer[] polygonSymbolizers = createPolygonSymbolizarts(this);
        rule.setSymbolizers(polygonSymbolizers);
        getDefaultFeatureTypeStyle(style).addRule(rule);

        // create MultiPolygon rule
        rule = styleBuilder.createRule(new Symbolizer[]{});
        filter = createGeometryFunctionFilter(geomXPath, MultiPolygon.class.getSimpleName());
        rule.setFilter(filter);
        rule.setSymbolizers(polygonSymbolizers);
        getDefaultFeatureTypeStyle(style).addRule(rule);
    }

    /**
     * @return the first rule in the default feature type style.
     */
    public Rule getDefaultRule( Style style ) {
        FeatureTypeStyle ftStyle = getDefaultFeatureTypeStyle(style);
        if (ftStyle.getRules() == null || ftStyle.getRules().length == 0) {
            // create an empty rule
            Rule rule = styleBuilder.createRule(new Symbolizer[]{});
            ftStyle.addRule(rule);
            return rule;
        }

        return ftStyle.getRules()[0];
    }

    /**
     * Returns the the first feature type style for the SLD style. The feature type style is created
     * if it does not exist.
     * 
     * @return The default (first) feature type style.
     */
    public FeatureTypeStyle getDefaultFeatureTypeStyle( Style style ) {
        List< ? extends org.opengis.style.FeatureTypeStyle> fts = style.featureTypeStyles();
        FeatureTypeStyle[] styles = style.getFeatureTypeStyles();
        if (styles == null || styles.length == 0 || styles[0].getRules().length == 0) {
            // create a feature type style
            FeatureTypeStyle ftStyle = styleBuilder.createFeatureTypeStyle("default", new Rule[]{}); //$NON-NLS-1$
            style.addFeatureTypeStyle(ftStyle);
            return ftStyle;
        }

        return styles[0];
    }

    protected PointSymbolizer createPointSymbolizer( Color colour, int size ) {
        Fill fill = styleBuilder.createFill(Color.YELLOW, 0.0);
        Stroke stroke = builder.createStroke(colour, 2);
        Mark mark = styleBuilder.createMark("square", fill, stroke); //$NON-NLS-1$

        Graphic graph2 = styleBuilder.createGraphic(null, mark, null, 1, size, 0);
        PointSymbolizer symb = styleBuilder.createPointSymbolizer(graph2);

        symb.getGraphic().setMarks(new Mark[]{mark});

        return symb;
    }

    protected LineSymbolizer createLineSymbolizer( Color colour, boolean dashed ) {
        return createLineSymbolizer(colour, dashed, 1, 1.0);
    }
    protected LineSymbolizer createLineSymbolizer( Color colour, boolean dashed, int thickness,
            double opacity ) {
        LineSymbolizer symbolizer = styleBuilder.createLineSymbolizer();

        Stroke stroke = builder.createStroke(colour, thickness, opacity);
        // if( dashed ){
        // stroke.setDashArray(new float[]{5, 3});
        // }
        symbolizer.setStroke(stroke);

        return symbolizer;
    }

    protected PolygonSymbolizer createPolygonSymbolizer( Color colour, boolean dashed ) {
        PolygonSymbolizer symbolizer = styleBuilder.createPolygonSymbolizer();

        // Stroke stroke = builder.createStroke(Color., 3);
        // if( dashed ){
        // stroke.setDashArray(new float[]{5, 3});
        // }
        // symbolizer.setStroke(stroke);

        Fill fill = styleBuilder.createFill();
        fill.setColor(styleBuilder.colorExpression(colour));
        fill.setOpacity(styleBuilder.literalExpression(.5));
        symbolizer.setFill(fill);

        return symbolizer;
    }

    /*
     * @see net.refractions.udig.project.StyleContent#load(java.net.URL)
     */
    public Object load( URL url, IProgressMonitor monitor ) throws IOException {
        return null;
    }

    /**
     * Creates an SLD and UserLayer, and nests the style (SLD-->UserLayer-->Style). This method is a
     * copy of the one in SLDContent to avoid a cyclic dependency.
     * 
     * @see net.refractions.style.sld.SLDContent#createDefaultStyledLayerDescriptor
     * @param style
     * @return SLD
     */
    private static StyledLayerDescriptor createDefaultStyledLayerDescriptor( Style style ) {
        StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();
        UserLayer layer = styleFactory.createUserLayer();
        sld.addStyledLayer(layer);
        layer.addUserStyle(style);
        return sld;
    }
    public static final boolean isPolygon( SimpleFeatureType featureType ) {
        if (featureType == null)
            return false;
        return isPolygon(featureType.getGeometryDescriptor());
    }
    /* This needed to be a function as it was being writen poorly everywhere */
    public static final boolean isPolygon( GeometryDescriptor geometryType ) {
        if (geometryType == null)
            return false;
        Class type = geometryType.getType().getBinding();
        return Polygon.class.isAssignableFrom(type) || MultiPolygon.class.isAssignableFrom(type);
    }
    public static final boolean isLine( SimpleFeatureType featureType ) {
        if (featureType == null)
            return false;
        return isLine(featureType.getGeometryDescriptor());
    }
    /* This needed to be a function as it was being writen poorly everywhere */
    public static final boolean isLine( GeometryDescriptor geometryType ) {
        if (geometryType == null)
            return false;
        Class type = geometryType.getType().getBinding();
        return LineString.class.isAssignableFrom(type)
                || MultiLineString.class.isAssignableFrom(type);
    }
    public static final boolean isPoint( SimpleFeatureType featureType ) {
        if (featureType == null)
            return false;
        return isPoint(featureType.getGeometryDescriptor());
    }
    /* This needed to be a function as it was being writen poorly everywhere */
    public static final boolean isPoint( GeometryDescriptor geometryType ) {
        if (geometryType == null)
            return false;
        Class type = geometryType.getType().getBinding();
        return Point.class.isAssignableFrom(type) || MultiPoint.class.isAssignableFrom(type);
    }
}