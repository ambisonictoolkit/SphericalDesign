PointView : View {
	var <points; // points should be Cartesians
	var <connections;
	var axisPnts;

	// drawing
	var <cen, <minDim;
	var skewX = 0, skewY = 0;
	var translateX = 0, translateY = 0;
	var az, bz = 3;              // perspective parameters, see originDist_, eyeDist_
	var showIndices = true;      // show indices of points
	var showAxes = true;         // show world axes
	var showConnections = false; // show connections between points
	var xyz, axisColors, axisScale = 1;

	// movement
	var rotate = 0, tilt = 0, tumble = 0;
	var autoRotate = false, autoTumble = false, autoTilt = false;
	var rotateStep;

	// views
	var <userView;

	// interaction
	var mouseDownPnt, mouseUpPnt, mouseMovePnt;


	*new { |parent, bounds = (Rect(0,0, 600, 500))|
		^super.new(parent, bounds).init;
	}

	init { |argSpec, initVal|

		points = [];
		az = bz + 1; // distance to point from eye
		rotateStep = 0.5.degrad; // if autoRotate

		userView = UserView(this, this.bounds.origin_(0@0))
		.resize_(5)
		.frameRate_(25)
		.drawFunc_(this.drawFunc)
		;

		// origin, x, y, z
		axisPnts = [[0,0,0], [1,0,0], [0,1,0], [0,0,1]].collect(_.asCartesian);
		axisColors = [\blue, \red, \green].collect{|col| Color.perform(col, 1, 0.7) };
		xyz = #["X", "Y", "Z"];

		// TODO:
		this.initInteractions;

		this.onResize_({ this.updateCanvasDims });

		this.onClose_({  }); // set default onClose to removeDependants

		// initialize canvas
		this.updateCanvasDims;
	}

	updateCanvasDims {
		var bnds;
		userView.bounds_(this.bounds.origin_(0@0));
		bnds = userView.bounds;
		cen  = bnds.center;
		minDim = min(bnds.width, bnds.height);
	}

	frameRate_ { |hz| userView.frameRate_(hz) }
	frameRate { |hz| userView.frameRate }

	drawFunc {
		^{ |v|
			var scale;
			var pnts, pnts_xf, pnt_depths;
			var axPnts, axPnts_xf, axPnts_depths;
			var rotPnts, to2D, incStep;
			var strRect;

			rotPnts = { |carts|
				carts.collect{ |pnt|
					pnt
					.rotate(0.5pi).tilt(0.5pi) // orient so view matches ambisonics
					.rotate(tilt).tilt(tumble).tumble(rotate) // user rotation
				};
			};

			// xformed points from 3D -> perspective -> 2D
			// + cart.z; accounts for depth adjusted by rotation
			// (az is the depth position of the _center_ of shape's rotation)
			// https://en.wikipedia.org/wiki/3D_projection
			to2D = { |carts|
				carts.collect{ |cart|
					(   cart
						+ (skewX @ skewY.neg)           // offset points within world, in normalized
						* (bz / (az + cart.z))          // add perspective
						+ (translateX @ translateY.neg) // translate the "world"
					).asPoint * scale                   // discard z for 2D drawing, scale to window size
				}
			};

			incStep = { |rotation| (rotation + rotateStep) % 2pi };

			scale = minDim.half;

			if (autoRotate) { rotate = incStep.(rotate) };
			if (autoTilt) { tilt = incStep.(tilt) };
			if (autoTumble) { tumble = incStep(tumble) };

			// rotate into ambisonics coords and rotate for user
			pnts = rotPnts.(points);
			axPnts = rotPnts.(axisPnts * axisScale);

			// hold on to these point depths (z) for use when drawing with perspective
			// pnt_depths = sin(pnts.collect(_.z) * 0.5pi); // warp depth with a sin function, why not?
			pnt_depths = pnts.collect(_.z);
			axPnts_depths = axPnts.collect(_.z);

			// transform 3D positions to 2D points with perspective
			pnts_xf = to2D.(pnts);
			axPnts_xf = to2D.(axPnts);

			/* DRAW */

			// move to center
			Pen.translate(cen.x, cen.y);

			// draw axes
			if (showAxes) {
				var lineDpth, pntDepth, pntSize;
				var r, oxy, theta;

				strRect = "XX".bounds.asRect;
				r = strRect.width / 2;

				// axPnts_xf = [origin,x,y,z]
				axPnts_xf[1..].do{ |axPnt, i|
					pntDepth = axPnts_depths[i+1];

					// average the depth between pairs of connected points
					lineDpth = axPnts_depths[0] + pntDepth * 0.5;
					pntSize = pntDepth.linlin(-1.0,1.0, 15, 5);

					Pen.strokeColor_(axisColors[i]);
					Pen.moveTo(axPnts_xf[0]);
					Pen.width_(lineDpth.linlin(-1.0,1.0, 4, 0.5));
					Pen.lineTo(axPnt);
					Pen.stroke;

					// draw axis label
					theta = atan2(axPnt.y - axPnts_xf[0].y, axPnt.x - axPnts_xf[0].x);
					oxy = Point(theta.cos, theta.sin) * r;
					strRect = strRect.center_(axPnt + oxy);

					Pen.fillColor_(axisColors[i]);
					Pen.stringCenteredIn(
						xyz[i],
						strRect,
						Font.default.pointSize_(
							pntDepth.linlin(-1.0,1.0, 18, 10) // change font size with depth
						)
					);

				};
			};

			// draw points
			strRect = "000000".bounds.asRect;
			pnts_xf.do{ |pnt, i|
				var pntSize;

				pntSize = pnt_depths[i].linlin(-1.0,1.0, 15, 5);

				// draw index
				if (showIndices) {
					Pen.fillColor_(Color.black);
					Pen.stringLeftJustIn(
						i.asString,
						strRect.left_(pnt.x + pntSize).bottom_(pnt.y + pntSize),
						Font.default.pointSize_(
							pnt_depths[i].linlin(-1.0,1.0, 18, 10) // change font size with depth
						)
					);
				};

				// draw point
				Pen.fillColor_(Color.hsv(i / (pnts_xf.size - 1), 1, 1));
				Pen.fillOval(Size(pntSize, pntSize).asRect.center_(pnt));
			};

			// draw connecting lines
			if (showConnections and: { connections.notNil }) {
				connections.do{ |set, i|
					var pDpths;

					// collect and average the depth between pairs of connected points
					pDpths = set.collect(pnt_depths[_]);
					pDpths = pDpths + pDpths.rotate(-1) / 2;

					Pen.strokeColor_(Color.blue.alpha_(0.1));
					Pen.moveTo(pnts_xf.at(set[0]));

					set.rotate(-1).do{ |idx, j|
						// change line width with depth
						Pen.width_(pDpths[j].linlin(-1.0,1.0, 4, 0.5));
						Pen.lineTo(pnts_xf[idx]);
						// stroke in the loop to retain independent
						// line widths within the set
						Pen.stroke;
						Pen.moveTo(pnts_xf[idx]);
					};
				};

			};
		}
	}

	// skew/offset the points in the world (before perspective is added)
	skewX_ { |norm = 0.0|
		skewX = norm;
		this.refresh;
	}
	skewY_ { |norm = 0.0|
		skewY = norm;
		this.refresh;
	}

	// translate the world (after perspective is added)
	translateX_ { |norm|   // translateX: left -> right = -1 -> 1
		translateX = norm;
		this.refresh;
	}
	translateY_ { |norm|   // translateY: bottom -> top = -1 -> 1
		translateY = norm;
		this.refresh;
	}

	// distance of points' origin to screen
	originDist_ { |norm|
		az = bz + norm;
		this.refresh;
	}

	// distance of eye to screen
	eyeDist_ { |norm|
		var temp = az - bz; // store origin offset
		bz = norm;
		az = bz + temp;
		this.refresh;
	}

	autoRotate_ { |bool|
		autoRotate = bool;
		userView.animate_(bool)
	}

	showIndices_ { |bool|
		showIndices = bool;
		this.refresh;
	}

	showAxes_ { |bool|
		showAxes = bool;
		this.refresh;
	}

	showConnections_ { |bool|
		showConnections = bool;
		this.refresh;
	}

	// draw lines between these indices of points
	// e.g. [[1,3],[0,5],[2.4]]
	connections_ { |arraysOfIndices|
		if (arraysOfIndices.rank != 2) {
			"[PointView:-connections_] arraysOfIndices argument "
			"is not an array with rank == 2.".throw
		};

		connections = arraysOfIndices;
		showConnections = true;
		this.refresh;
	}

	axisColors_ { |colorArray|
		axisColors = colorArray;
		this.refresh;
	}

	axisScale_ { |scale|
		axisScale = scale;
		this.refresh;
	}

	// TODO:
	initInteractions {
		userView.mouseMoveAction_({
			|v,x,y,modifiers|
			mouseMovePnt = x@y;
			// mouseMoveAction.(v,x,y,modifiers)
		});

		userView.mouseDownAction_({
			|v,x,y, modifiers, buttonNumber, clickCount|
			mouseDownPnt = x@y;
			// mouseDownAction.(v,x,y, modifiers, buttonNumber, clickCount)
		});

		userView.mouseUpAction_({
			|v,x,y, modifiers|
			mouseUpPnt = x@y;
			// mouseUpAction.(v,x,y,modifiers)
		});

		userView.mouseWheelAction_({
			|v, x, y, modifiers, xDelta, yDelta|
			// this.stepByScroll(v, x, y, modifiers, xDelta, yDelta);
		});

		// NOTE: if overwriting this function, include a call to
		// this.stepByArrowKey(key) to retain key inc/decrement capability
		userView.keyDownAction_ ({
			|view, char, modifiers, unicode, keycode, key|
			// this.stepByArrowKey(key);
		});

	}

	points_ { |cartesians|
		points = cartesians;
		connections = [(0..points.size-1)];
		this.refresh;
	}

	// Set points by directions.
	// Can be an Array of:
	// Sphericals, or
	// [[theta, phi], [theta, phi] ...], (rho assumed to be 1) or
	// [[theta, phi, rho], [theta, phi, rho] ...]
	directions_ { |dirArray|
		var first, sphericals;

		first = dirArray[0];
		sphericals = case
		{ first.isKindOf(Spherical) } {
			dirArray
		}
		{ first.size ==  2 } {
			dirArray.collect( Spherical(1, *_) )
		}
		{ first.size ==  3 } {
			dirArray.collect{ |tpr| tpr.postln; Spherical(tpr[2], tpr[0], tpr[1]) }
		}
		{
			"[PointView:-directions_] Invalid dirArray argument."
			"Can be an Array of: Sphericals, or [[theta, phi], [theta, phi] ...], "
			"(rho assumed to be 1), or [[theta, phi, rho], [theta, phi, rho] ...]"
			.throw
		};

		this.points_(sphericals.collect(_.asCartesian));
	}

	refresh {
		userView.animate.not.if{ userView.refresh };
	}

}

/*

Usage

(
// p = TDesign(25).points;
p = [
	[0,0,1], [0,0,-1],
	[0,1,0], [0,-1,0],
	[1,0,0], [-1,0,0],
].collect(_.asCartesian);
v = PointView(bounds: [0,0, 400, 500].asRect).front.points_(p);
v.eyeDist = 2;
v.originDist = 2;
v.autoRotate = true;
v.showIndices = true;
v.showAxes = true;
)

v.skewX = 0.85;
v.skewY = 0.85;
v.eyeDist = 1.5;
v.originDist = 2.8;

v.eyeDist = 1;
v.originDist = 1;


(
d = [
	[ -18, 0 ], [ -54, 0 ], [ -90, 0 ], [ -126, 0 ], [ -162, 0 ], [ -198, 0 ], [ -234, 0 ], [ -270, 0 ], [ -306, 0 ], [ -342, 0 ],
	[ 0, -10 ], [ -72, -10 ], [ -144, -10 ], [ -216, -10 ], [ -288, -10 ],
	[ -45, 45 ], [ -135, 45 ], [ -225, 45 ], [ -315, 45 ], [ 0, 90 ]
];
a = VBAPSpeakerArray.new(3, d);
a.choose_ls_triplets;
p = d.degrad.collect({ |dir| Spherical(1, *dir).asCartesian});
// v = PointView(bounds: [0,0, 400, 500].asRect).front.points_(p);
v = PointView().front.points_(p);
v.eyeDist = 2;
v.originDist = 2;
v.autoRotate = true;
v.showIndices = true;
v.connections = a.sets.collect(_.chanOffsets.asInt);
v.translateY = 0.35;
v.skewY = -0.85;
v.frameRate = 135;
v.axisScale = 0.333;
)

// TODO:

// controls:
// - RTT controls
// - auto RTT with range/rate params
// - show axes
// - eye distance (perspective)
// features:
// - normalize input points (for drawing, not display)
// - orthogonal views: looking +/- XYZ axes (XY plane, XZ plane, etc.)
// - draw mesh in bundled points
// - specify point colors in groups or individually
// enhancements:
// - draw furthest points first so closer points drawn on top
// - break lines it segments to accentuate distance (axes especially)
// - show point info on mouseOver
*/