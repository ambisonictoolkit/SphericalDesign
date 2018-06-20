PointView : View {
	var <points; // points should be Cartesians
	// drawing
	var <bnds, <cen, <minDim, <canvas;
	var skewX = 0, skewY = 0;
	var shiftX = 0, shiftY = 0;
	var bz = 1.2; // distance to screen
	var az;       // distance to point from eye
	var <showIndices = true; // show indices of points
	var showAxes = true;     // show world axes

	var rotate = 0, tilt = 0, tumble = 0;
	var autoRotate = false, autoTumble = false, autoTilt = false;
	var rotateStep;
	var axisPnts;

	// views
	var <userView;
	// interaction
	var mouseDownPnt, mouseUpPnt, mouseMovePnt;


	*new { |parent, bounds|
		^super.new(parent, bounds).init;
	}

	init { |argSpec, initVal|

		points = [];
		az = bz + 0.2; // distance to point from eye
		rotateStep = 0.5.degrad; // if autoRotate

		userView = UserView(this, this.bounds.origin_(0@0))
		.resize_(5)
		.frameRate_(30)
		.drawFunc_(this.drawFunc)
		;

		axisPnts = [
			[1,0,0],
			[0,1,0],
			[0,0,1],
		].collect(_.asCartesian);

		// TODO:
		this.initInteractions;

		this.onResize_({ this.updateCanvasDims });

		this.onClose_({  }); // set default onClose to removeDependants

		// initialize canvas
		this.updateCanvasDims;
	}

	updateCanvasDims {
		userView.bounds_(this.bounds.origin_(0@0));
		bnds = userView.bounds;
		cen  = bnds.center;
		minDim = min(bnds.width, bnds.height);
		canvas = Size(minDim, minDim).asRect.center_(cen);
	}

	frameRate_ { |hz| userView.frameRate_(hz) }
	frameRate { |hz| userView.frameRate }



	drawFunc {
		^{ |v|
			var halfW, halfH;
			var scale;
			var pnts, pnts_xf, pnt_depths;
			var axPnts, axPnts_xf, axPnts_depths;
			var rotPnts, to2D, incStep;

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
			to2D = { |carts|
				carts.collect{ |cart|
					// offset points within world, in normalized
					// coords (1 is max point boundary)
					cart = cart + (skewX @  skewY.neg);

					(cart * (bz / (az + cart.z)))  // add perspective
					+ (shiftX @ shiftY.neg)        // translate the "world"
					* Cartesian(scale, scale, 1)   // scale to window size
					.asPoint                       // discard z for 2D drawing
				}
			};

			incStep = { |rotation| (rotation + rotateStep) % 2pi };

			scale = minDim.half;
			// #halfW, halfH = [bnds.width.half, bnds.height.half];

			if (autoRotate) { rotate = incStep.(rotate) };
			if (autoTilt) { tilt = incStep.(tilt) };
			if (autoTumble) { tumble = incStep(tumble) };

			// rotate into ambisonics coords and rotate for user
			pnts = rotPnts.(points);
			axPnts = rotPnts.(axisPnts);

			// hold on to these point depths (z) for use when drawing with perspective
			pnt_depths = sin(pnts.collect(_.z) * 0.5pi); // warp depth with a sin function, why not?
			axPnts_depths = sin(axPnts.collect(_.z) * 0.5pi); // warp depth with a sin function, why not?
			// 4.postln;

			pnts_xf = to2D.(pnts);
			axPnts_xf = to2D.(axPnts);

			/* DRAW */
			Pen.translate(cen.x, cen.y); // move to center

			// 5.postln;
			// TODO: draw farthest points first
			pnts_xf.do{ |pnt, i|
				var pntsize;
				Pen.fillColor_(Color.hsv(i / (pnts_xf.size - 1), 1, 1));
				pntsize = pnt_depths[i].linlin(-1.0,1.0, 15, 5);

				// draw point
				Pen.fillOval(Size(pntsize,pntsize).asRect.center_(pnt));
				// draw index
				if (showIndices) {
					Pen.fillColor_(Color.black);
					Pen.stringLeftJustIn(
						i.asString,
						"-000.0".bounds.asRect.left_(pnt.x+pntsize).bottom_(pnt.y+pntsize),
						Font.default.pointSize_(
							pnt_depths[i].linlin(-1.0,1.0, 18, 10) // change font size with depth
						)
					);
				};
			};

			// a.sets.do{|set, i|
			// 	var spks, pntd;
			// 	spks = set.chanOffsets;
			//
			// 	Pen.moveTo(pnts_xf[spks[0]]);
			//
			// 	pntd = spks.collect({|idx| pnt_depths[idx]});
			// 	pntd = pntd + pntd.rotate(-1) / 2; // average depth between connected points
			//
			// 	Pen.strokeColor_(Color.blue.alpha_(0.1));
			//
			// 	spks.rotate(-1).do{|idx, j|
			// 		// Pen.width_(pnts[idx].z.linlin(-1.0,1.0, 5, 0.5)); // change line width with depth
			// 		Pen.width_(pntd[j].linlin(-1.0,1.0, 4, 0.5)); // change line width with depth
			// 		Pen.lineTo(pnts_xf[idx]);
			// 	};
			// 	Pen.stroke;
			// };
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

	// shift/translate the world (after perspective is added)
	shiftX_ { |norm|   // shiftX: left -> right = -1 -> 1
		shiftX = norm;
		this.refresh;
	}
	shiftY_ { |norm|   // shiftY: bottom -> top = -1 -> 1
		shiftY = norm;
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
		this.refresh;
	}

	refresh {
		userView.animate.not.if{ userView.refresh };
	}

	// // overwrite default View method to retain freeing dependants
	// onClose_ { |func|
	// 	var newFunc = { |...args|
	// 		layers.do(_.removeDependant(this));
	// 		func.(*args)
	// 	};
	// 	// from View:onClose_
	// 	this.manageFunctionConnection( onClose, newFunc, 'destroyed()', false );
	// 	onClose = newFunc;
	// }
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

*/