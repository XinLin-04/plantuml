/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2024, Arnaud Roques
 *
 * Project Info:  https://plantuml.com
 *
 * If you like this project or if you find it useful, you can support us at:
 *
 * https://plantuml.com/patreon (only 1$ per month!)
 * https://plantuml.com/paypal
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 *
 */
package net.sourceforge.plantuml.timingdiagram.graphic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.plantuml.klimt.Fashion;
import net.sourceforge.plantuml.klimt.UStroke;
import net.sourceforge.plantuml.klimt.UTranslate;
import net.sourceforge.plantuml.klimt.color.Colors;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.creole.Display;
import net.sourceforge.plantuml.klimt.drawing.UGraphic;
import net.sourceforge.plantuml.klimt.font.FontConfiguration;
import net.sourceforge.plantuml.klimt.font.StringBounder;
import net.sourceforge.plantuml.klimt.geom.HorizontalAlignment;
import net.sourceforge.plantuml.klimt.geom.XDimension2D;
import net.sourceforge.plantuml.klimt.geom.XPoint2D;
import net.sourceforge.plantuml.klimt.shape.AbstractTextBlock;
import net.sourceforge.plantuml.klimt.shape.TextBlock;
import net.sourceforge.plantuml.klimt.shape.UDrawable;
import net.sourceforge.plantuml.klimt.shape.ULine;
import net.sourceforge.plantuml.klimt.shape.URectangle;
import net.sourceforge.plantuml.style.ISkinParam;
import net.sourceforge.plantuml.style.PName;
import net.sourceforge.plantuml.style.Style;
import net.sourceforge.plantuml.timingdiagram.ChangeState;
import net.sourceforge.plantuml.timingdiagram.TimeConstraint;
import net.sourceforge.plantuml.timingdiagram.TimeTick;
import net.sourceforge.plantuml.timingdiagram.TimingRuler;

public class Histogram implements PDrawing {

	private final List<ChangeState> changes = new ArrayList<>();
	private final List<TimeConstraint> constraints = new ArrayList<>();

	private List<String> allStates;

	private final ISkinParam skinParam;
	private final TimingRuler ruler;
	private final boolean compact;
	private String initialState;
	private final TextBlock title;
	private final int suggestedHeight;
	private final Style style;
	private final Style style0;

	public Histogram(TimingRuler ruler, ISkinParam skinParam, Collection<String> someStates, boolean compact,
			TextBlock title, int suggestedHeight, Style style, Style style0) {
		this.style = style;
		this.style0 = style0;
		this.suggestedHeight = suggestedHeight;
		this.ruler = ruler;
		this.skinParam = skinParam;
		this.allStates = new ArrayList<>(someStates);
		this.compact = compact;
		this.title = title;
		Collections.reverse(allStates);
	}

	public IntricatedPoint getTimeProjection(StringBounder stringBounder, TimeTick tick) {
		final double x = ruler.getPosInPixel(tick);
		final List<String> states = getStatesAt(tick);
		if (states.size() == 0)
			return null;

		final double heightForConstraints = getHeightForConstraints(stringBounder);
		if (states.size() == 1) {
			final double y = yOfState(states.get(0)) + heightForConstraints;
			return new IntricatedPoint(new XPoint2D(x, y), new XPoint2D(x, y));
		}
		assert states.size() == 2;
		final double y1 = yOfState(states.get(0)) + heightForConstraints;
		final double y2 = yOfState(states.get(1)) + heightForConstraints;
		assert y1 != y2;
		return new IntricatedPoint(new XPoint2D(x, y1), new XPoint2D(x, y2));
	}

	private List<String> getStatesAt(TimeTick tick) {
		if (changes.size() == 0)
			return Collections.emptyList();

		for (int i = 0; i < changes.size(); i++) {
			final int tickWithCurrentChangeTimeComparisonResult = changes.get(i).getWhen().compareTo(tick);
			if (tickWithCurrentChangeTimeComparisonResult == 0) {
				if (i == 0 && initialState == null)
					return Arrays.asList(changes.get(i).getState());

				if (i == 0 && initialState != null)
					return Arrays.asList(initialState, changes.get(i).getState());

				return Arrays.asList(changes.get(i - 1).getState(), changes.get(i).getState());
			}
			if (tickWithCurrentChangeTimeComparisonResult > 0) {
				final int changeIndex;
				if (i == 0) {
					// if this time tick was not yet defined in any place, and is less then the
					// first one,
					// assume it's the leftmost
					changeIndex = 0;
				} else {
					changeIndex = i - 1;
				}
				return Collections.singletonList(changes.get(changeIndex).getState());
			}
		}
		return Collections.singletonList(changes.get(changes.size() - 1).getState());
	}

	public void addChange(ChangeState change) {
		changes.add(change);
		if (change.isCompletelyHidden())
			return;

		final List<String> states = change.getStates();
		for (String state : states)
			if (allStates.contains(state) == false)
				allStates.add(state);

	}

	private XPoint2D[] getPoints(int n) {
		final ChangeState change = changes.get(n);
		final double x = ruler.getPosInPixel(change.getWhen());
		final List<String> states = change.getStates();
		if (states.size() == 2)
			return new XPoint2D[] { new XPoint2D(x, yOfState(states.get(0))),
					new XPoint2D(x, yOfState(states.get(1))) };

		return new XPoint2D[] { new XPoint2D(x, yOfState(states.get(0))) };
	}

	private double getPointx(int n) {
		final ChangeState change = changes.get(n);
		return ruler.getPosInPixel(change.getWhen());
	}

	private double getPointMinY(int n) {
		final List<String> states = changes.get(n).getStates();
		if (states.size() == 2)
			return Math.min(yOfState(states.get(0)), yOfState(states.get(1)));

		return yOfState(states.get(0));
	}

	private double getPointMaxY(int n) {
		final List<String> states = changes.get(n).getStates();
		if (states.size() == 2)
			return Math.max(yOfState(states.get(0)), yOfState(states.get(1)));

		return yOfState(states.get(0));
	}

	private FontConfiguration getFontConfiguration() {
		return FontConfiguration.create(skinParam, style);
	}

	private UStroke getStroke() {
		return style.getStroke();
	}

	private Fashion getContext() {
		final HColor lineColor = style.value(PName.LineColor).asColor(skinParam.getIHtmlColorSet());
		final HColor backgroundColor = style.value(PName.BackGroundColor).asColor(skinParam.getIHtmlColorSet());

		return new Fashion(backgroundColor, lineColor).withStroke(getStroke());
	}

	public TextBlock getPart1(final double fullAvailableWidth) {
		return new AbstractTextBlock() {
			public void drawU(UGraphic ug) {
				drawPart1(ug, fullAvailableWidth);
			}

			public XDimension2D calculateDimension(StringBounder stringBounder) {
				return calculateDimensionPart1(stringBounder);
			}
		};
	}

	public UDrawable getPart2() {
		return new UDrawable() {
			public void drawU(UGraphic ug) {
				drawPart2(ug);
			}
		};
	}

	private XDimension2D calculateDimensionPart1(StringBounder stringBounder) {
		double width = 0;
		for (String state : allStates) {
			final TextBlock label = getTextBlock(state);
			final XDimension2D dim = label.calculateDimension(stringBounder);
			width = Math.max(width, dim.getWidth());
		}
		if (initialState != null)
			width += getInitialWidth();

		if (compact)
			width += title.calculateDimension(stringBounder).getWidth() + 15;

		return new XDimension2D(width, getFullHeight(stringBounder));
	}

	private void drawPart1(UGraphic ug, double fullAvailableWidth) {
		final StringBounder stringBounder = ug.getStringBounder();
		ug = ug.apply(UTranslate.dy(getHeightForConstraints(stringBounder)));

		if (compact) {
			final double titleHeight = title.calculateDimension(stringBounder).getHeight();
			final double dy = (getFullHeight(stringBounder) - titleHeight) / 2;
			title.drawU(ug.apply(UTranslate.dy(dy)));
		}
		double width = getStatesWidth(stringBounder);
		if (initialState != null)
			width += getInitialWidth();

		if (fullAvailableWidth > width + 5)
			ug = ug.apply(UTranslate.dx(fullAvailableWidth - width - 5));
		else
			ug = ug.apply(UTranslate.dx(fullAvailableWidth - width));
		for (String state : allStates) {
			final TextBlock label = getTextBlock(state);
			final XDimension2D dim = label.calculateDimension(stringBounder);
			label.drawU(ug.apply(UTranslate.dy(yOfState(state) - dim.getHeight() / 2 + 1)));
		}
	}

	private double getStatesWidth(StringBounder stringBounder) {
		double result = 0;
		for (String state : allStates) {
			final TextBlock label = getTextBlock(state);
			final XDimension2D dim = label.calculateDimension(stringBounder);
			result = Math.max(result, dim.getWidth());
		}
		return result;
	}

	private void drawPart2(UGraphic ug) {
		if (changes.size() == 0)
			return;

		ug = getContext().apply(ug);
		ug = ug.apply(UTranslate.dy(getHeightForConstraints(ug.getStringBounder())));
		drawHlines(ug);
		drawVlines(ug);
		drawLabels(ug);
		drawConstraints(ug.apply(UTranslate.dy(-TimeConstraint.getTopMargin())));
	}

	private void drawHlines(UGraphic ug) {
		if (initialState != null)
			for (XPoint2D pt : getPoints(0))
				drawHLine(ug, getInitialPoint(), getInitialWidth() + pt.getX());

		for (int i = 0; i < changes.size(); i++) {
			if (changes.get(i).isCompletelyHidden())
				continue;

			final double x2 = i < changes.size() - 1 ? getPointx(i + 1) : ruler.getWidth();
			final double len = x2 - getPointx(i);
			final XPoint2D[] points = getPoints(i);
			if (points.length == 2)
				drawHBlock(ug.apply(changes.get(i).getBackColor(skinParam, style).bg()), points[0], points[1], len);

			if (i < changes.size() - 1)
				for (XPoint2D pt : points)
					drawHLine(ug, pt, len);

		}

		if (changes.get(changes.size() - 1).isCompletelyHidden() == false) {
			for (XPoint2D pt : getPoints(changes.size() - 1)) {
				final double len = ruler.getWidth() - pt.getX();
				drawHLine(ug, pt, len);
			}
		}
	}

	private void drawHBlock(UGraphic ug, XPoint2D pt1, XPoint2D pt2, double len) {
		final double minY = Math.min(pt1.getY(), pt2.getY());
		final double maxY = Math.max(pt1.getY(), pt2.getY());
		final XPoint2D pt = new XPoint2D(pt1.getX(), minY);
		ug = ug.apply(UTranslate.point(pt));
		ug.draw(URectangle.build(len, maxY - minY));
		for (double x = 0; x < len; x += 5)
			ug.apply(UTranslate.dx(x)).draw(ULine.vline(maxY - minY));

	}

	private void drawHLine(UGraphic ug, final XPoint2D pt, final double len) {
		ug.apply(UTranslate.point(pt)).draw(ULine.hline(len));
	}

	private void drawVlines(UGraphic ug) {
		if (initialState != null) {
			final XPoint2D before = getInitialPoint();
			final XPoint2D current = getPoints(0)[0];
			ug.apply(UTranslate.point(current)).draw(ULine.vline(before.getY() - current.getY()));
		}
		for (int i = 1; i < changes.size(); i++) {
			if (changes.get(i - 1).isCompletelyHidden() || changes.get(i).isCompletelyHidden())
				continue;

			final double minY = Math.min(getPointMinY(i), getPointMinY(i - 1));
			final double maxY = Math.max(getPointMaxY(i), getPointMaxY(i - 1));
			ug.apply(new UTranslate(getPointx(i), minY)).draw(ULine.vline(maxY - minY));
		}
	}

	private void drawLabels(UGraphic ug) {
		for (int i = 0; i < changes.size(); i++) {
			final XPoint2D ptLabel = getPoints(i)[0];
			final String comment = changes.get(i).getComment();
			if (comment == null)
				continue;

			final TextBlock label = getTextBlock(comment);
			final XDimension2D dim = label.calculateDimension(ug.getStringBounder());
			label.drawU(ug.apply(UTranslate.point(ptLabel).compose(new UTranslate(2, -dim.getHeight()))));
		}
	}

	private void drawConstraints(UGraphic ug) {
		for (TimeConstraint constraint : constraints) {
			double y = yOfState(constraint.getTick1());
			for (ChangeState change : changes)
				if (constraint.containsStrict(change.getWhen()))
					y = Math.min(y, yOfState(change.getWhen()));

			constraint.drawU(ug.apply(UTranslate.dy(y)), ruler);
		}
	}

	private XPoint2D getInitialPoint() {
		return new XPoint2D(-getInitialWidth(), yOfState(initialState));
	}

	private double getHeightForConstraints(StringBounder stringBounder) {
		return TimeConstraint.getHeightForConstraints(stringBounder, constraints);
	}

	public double getFullHeight(StringBounder stringBounder) {
		double height = getHeightForConstraints(stringBounder);

		if (allStates.size() > 0)
			height += stepHeight() * (allStates.size() - 1);

		height += getBottomMargin();
		return height;
	}

	private double getBottomMargin() {
		return 12;
	}

	private double yOfState(String state) {
		final int nb = allStates.size() - 1 - allStates.indexOf(state);
		return stepHeight() * nb;
	}

	private double yOfState(TimeTick when) {
		return yOfState(last(getStatesAt(when)));
	}

	private static String last(List<String> list) {
		return list.get(list.size() - 1);
	}

	private double stepHeight() {
		if (suggestedHeight == 0 || allStates.size() <= 1)
			return 20;

		return suggestedHeight / (allStates.size() - 1);
	}

	private TextBlock getTextBlock(String value) {
		final Display display = Display.getWithNewlines(skinParam.getPragma(), value);
		return display.create(getFontConfiguration(), HorizontalAlignment.LEFT, skinParam);
	}

	public void setInitialState(String initialState, Colors initialColors) {
		this.initialState = initialState;
		if (initialState != null && allStates.contains(initialState) == false)
			allStates.add(initialState);

	}

	private double getInitialWidth() {
		return 40;
	}

	public void addConstraint(TimeConstraint constraint) {
		this.constraints.add(constraint);
	}

}
