PointView : View {
	var <points; // points should be Cartesians
	var <connections;
	var axisPnts;

	// drawing
	var <cen, <minDim;
	var <skewX = 0, <skewY = -0.95;
	var <translateX = 0, <translateY = 0.8;
	var az, bz = 3;               // perspective parameters, see originDist_, eyeDist_
	var <showIndices = true;      // show indices of points
	var <showAxes = true;         // show world axes
	var <showConnections = false; // show connections between points
	var perspective = true, ortho = false, orthoAxis = '+X';
	var xyz, <axisColors, <axisScale = 0.2;
	var frameRate = 25;
	var <pointColors, prPntDrawCols;
	var colsByHue = true, huesScrambled = false;     // if the colors have been set by hue range
	var <pointSize = 15, <pointDistScale = 0.333;

	// movement
	var <baseRotation, <baseTilt, <baseTumble; // radians, rotations before any movement offsets are applied
	var <rotate, <tilt, <tumble;               // radians, rotations after any movement offsets are applied
	var <rotateRate, <tiltRate, <tumbleRate;   // Hz
	var <rotateStep, tiltStep, tumbleStep;     // radians
	var <rotateDir, <tiltDir, <tumbleDir;      // +/-1
	var <cycRotate, <cycTilt, <cycTumble;
	var <oscRotate, <oscTumble, <oscTilt;
	var rotateOscT, rotateOscPhsInc;
	var tiltOscT, tiltOscPhsInc;
	var tumbleOscT, tumbleOscPhsInc;
	var <rotateOscWidth, tiltOscWidth, tumbleOscWidth;
	var >rotatePhase, >tiltPhase, >tumblePhase; // phase index into the rotation oscillators
	var <rotateMode;      // \rtt or \ypr
	var <randomizedAxes;  // dictionary of booleans for randomize state of each axis
	var <>randomVariance; // normalized value to apply to movement speed if randomized

	// views
	var <userView, <rotationView, <showView, <perspectiveView;

	// interaction
	var mouseDownPnt, mouseUpPnt, mouseMovePnt;

	*new { |parent, bounds = (Rect(0,0, 600, 500))|
		^super.new(parent, bounds).init;
	}

	init { |argSpec, initVal|
		var initOscWidth = 8.degrad;

		points = [];
		az = bz + 1; // distance to point from eye

		// init  vars
		tumbleRate = tiltRate = rotateRate = 30.reciprocal;
		baseRotation = -45.degrad;
		baseTilt = baseTumble = 0;
		rotate = tilt = tumble = 0;
		rotateDir = tiltDir = tumbleDir = 1;
		cycRotate = cycTumble = cycTilt = false;
		oscRotate = oscTumble = oscTilt = false;
		rotatePhase = tiltPhase = tumblePhase = 0;
		rotateMode = \rtt;
		randomVariance = 0.15;

		userView = UserView(this, this.bounds.origin_(0@0))
		.resize_(5)
		.frameRate_(frameRate)
		.drawFunc_(this.drawFunc)
		;

		// origin, x, y, z
		axisPnts = [[0,0,0], [1,0,0], [0,1,0], [0,0,1]].collect(_.asCartesian);
		axisColors = [\blue, \red, \green].collect{ |col| Color.perform(col, 1, 0.7) };
		xyz = #["X", "Y", "Z"];

		// init draw colors
		prPntDrawCols = [Color.hsv(0,1,1,1), Color.hsv(0.999,1,1,1)];
		colsByHue = true;
		randomizedAxes = IdentityDictionary(know: true).putPairs([
			\rotate, true,
			\tilt,   true,
			\tumble, true
		]);

		// init movement variables
		this.rotateOscPeriod_(this.rotatePeriod); // * (initOscWidth/2pi));
		this.tiltOscPeriod_(this.tiltPeriod); // * (initOscWidth/2pi));
		this.tumbleOscPeriod_(this.tumblePeriod); // * (initOscWidth/2pi));
		// rotateOscCenter = tiltOscCenter = tumbleOscCenter = 0;
		tumbleOscWidth = tiltOscWidth = rotateOscWidth = initOscWidth;
		this.rotateMode_(rotateMode);

		// init rotation variables
		this.rotateRate_(rotateRate);
		this.tiltRate_(tiltRate);
		this.tumbleRate_(tumbleRate);

		// TODO:
		this.initInteractions;

		this.onResize_({ this.updateCanvasDims });
		// initialize canvas
		this.updateCanvasDims;

		// init controller view
		rotationView = PointViewUI(this, Rect(5, 35, 405, 700));
		this.addDependant(rotationView);
		rotationView.onClose({ this.removeDependant(rotationView) });

		this.makeShowView;
		this.makePerspectiveView;

		this.layItOut;

	}

	layItOut {
		var rTxt, sTxt, pTxt;
		var onCol = Color.blue;
		var offCol = Color.gray;
		var tempTransX;

		tempTransX = translateX;

		this.layout_(
			VLayout(
				HLayout(
					rTxt = StaticText().string_("Rotation")
					.mouseDownAction_({ |txt|
						var cur;
						cur = rotationView.visible;
						if (cur) {
							txt.stringColor_(offCol);
							this.translateX_(tempTransX);
						} {
							txt.stringColor_(onCol);
							showView.visible_(false);
							perspectiveView.visible_(false);
							sTxt.stringColor_(offCol);
							pTxt.stringColor_(offCol);
							tempTransX = translateX;
							this.translateX_(0.6);
						};
						rotationView.visible_(cur.not);
					})
					.stringColor_(offCol),

					sTxt = StaticText().string_("Show")
					.mouseDownAction_({ |txt|
						var cur;
						cur = showView.visible;
						if (cur) {
							txt.stringColor_(offCol);
						} {
							txt.stringColor_(onCol);

							// return translation if rotation view is visible
							if (rotationView.visible) {
								this.translateX_(tempTransX);
							};
							rotationView.visible_(false);
							perspectiveView.visible_(false);
							rTxt.stringColor_(offCol);
							pTxt.stringColor_(offCol);
						};
						showView.visible_(cur.not)
					})
					.stringColor_(offCol),

					pTxt = StaticText().string_("Perspective")
					.mouseDownAction_({ |txt|
						var cur;
						cur = perspectiveView.visible;
						if (cur) {
							txt.stringColor_(offCol);
						} {
							txt.stringColor_(onCol);

							// return translation if rotation view is visible
							if (rotationView.visible) {
								this.translateX_(tempTransX);
							};
							showView.visible_(false);
							rotationView.visible_(false);
							sTxt.stringColor_(offCol);
							rTxt.stringColor_(offCol);
						};
						perspectiveView.visible_(cur.not)
					})
					.stringColor_(offCol),

					nil
				).spacing_(20),

				rotationView.maxWidth_(430).visible_(false),
				showView.maxWidth_(330).visible_(false),
				perspectiveView.maxWidth_(430).visible_(false),

				nil
			)
		);
		}

	makeShowView {
		var axChk, axLenSl;
		var indcChk;
		var connChk, triBut, seqBut;

		axChk = CheckBox()
		.action_({ |cb|
			this.showAxes_(cb.value);
		})
		.value_(showAxes)
		;

		axLenSl = Slider()
		.action_({ |sl|
			this.axisScale_(sl.value.linlin(0,1,0,1.5))
		})
		.orientation_(\horizontal)
		.value_(axisScale / 1.5);

		indcChk = CheckBox()
		.action_({ |cb|
			this.showIndices_(cb.value);
		})
		.value_(showIndices)
		;

		connChk = CheckBox()
		.action_({ |cb|
			this.showConnections_(cb.value);
		})
		.value_(showConnections)
		;

		triBut = Button()
		.action_({
			try {
				this.connections_(
					SphericalDesign().points_(points).calcTriplets.triplets
				)
			} {
				"Couldn't calulate the triangulation of points".warn
			};
			connChk.valueAction_(true);
		})
		.states_([["Triangulation"]])
		.maxHeight_(25)
		;

		seqBut = Button()
		.action_({
			this.connections_([(0..points.size-1)]);
			connChk.valueAction_(true);
		})
		.states_([["Sequential"]])
		.maxHeight_(25)
		;

		showView = View().layout_(
			VLayout(
				HLayout(
					axChk,
					StaticText().string_("Axes").align_(\left),
					15,
					StaticText().string_("length: ").align_(\left),
					axLenSl
				),
				HLayout(
					indcChk,
					StaticText().string_("Indices").align_(\left),
					nil
				),
				HLayout(
					connChk,
					StaticText().string_("Connections").align_(\left),
					10,
					triBut,
					10,
					seqBut
				),
			)
		);
	}

	makePerspectiveView {
		var xposChk, xnegChk, yposChk, ynegChk, zposChk, znegChk;
		var skxSl, skySl, trxSl, trySl;
		var orDistSl, eyeDistSl;

		#xposChk, xnegChk, yposChk, ynegChk, zposChk, znegChk =
		['+X', '-X', '+Y', '-Y', '+Z', '-Z'].collect{ |ax|
			CheckBox()
			.action_({ |cb|
				if (cb.value) {
					this.setOrtho(ax);
					[xposChk, xnegChk, yposChk, ynegChk, zposChk, znegChk].do({ |me|
						if (me != cb) {me.value = false}
					});
				} {
					this.setPerspective
				};
			})
			.value_(ortho and: { orthoAxis == ax })
		};

		#skxSl, skySl, trxSl, trySl =
		[\skewX, \skewY, \translateX, \translateY].collect{ |meth|
			var setter;
			setter = (meth ++ \_).asSymbol;
			Slider()
			.action_({ |sl| this.perform(setter, sl.value.linlin(0,1,-2,2)) })
			.value_(this.perform(meth).linlin(-2,2,0,1))
			.orientation_(\horizontal)
			.maxWidth_(200)
		};

		orDistSl = Slider()
		.action_({ |sl|
			this.originDist_(sl.value.linlin(0,1,0.5.neg,3))
		})
		.orientation_(\horizontal)
		.value_((az-bz).linlin(0.5.neg,3,0,1))
		;
		eyeDistSl = Slider()
		.action_({ |sl|
			this.eyeDist_(sl.value.linlin(0,1,1.5,6))
		})
		.orientation_(\horizontal)
		.value_(bz.linlin(1.5,6,0,1))
		;

		perspectiveView = View().layout_(
			HLayout(
				VLayout(
					StaticText().string_("Perspective").align_(\center),
					VLayout(
						StaticText().string_("Skew").align_(\left),
						HLayout(
							StaticText().string_("X"), skxSl
						),
						HLayout(
							StaticText().string_("Y"), skySl
						),
						StaticText().string_("Translate").align_(\left),
						HLayout(
							StaticText().string_("X"), trxSl
						),
						HLayout(
							StaticText().string_("Y"), trySl
						),
						StaticText().string_("Origin Distance").align_(\left),
						orDistSl,
						StaticText().string_("Eye Distance").align_(\left),
						eyeDistSl
					),
					nil
				),
				25,
				VLayout(
					StaticText().string_("Ortho").align_(\center),
					GridLayout.columns(
						[
							nil,
							StaticText().string_("X"),
							StaticText().string_("Y"),
							StaticText().string_("Z"),
						],
						[
							StaticText().string_("+").align_(\center), xposChk, yposChk, zposChk
						],
						[
							StaticText().string_("-").align_(\center), xnegChk, ynegChk, znegChk
						],
					),
					nil
				),
				nil
			)
		);
	}

	updateCanvasDims {
		var bnds;
		userView.bounds_(this.bounds.origin_(0@0));
		bnds = userView.bounds;
		cen  = bnds.center;
		minDim = min(bnds.width, bnds.height);
	}

	points_ { |cartesians|
		points = cartesians;
		connections = [(0..points.size-1)];
		this.prUpdateColors;
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
			dirArray.collect{ |tpr| Spherical(tpr[2], tpr[0], tpr[1]) }
		}
		{
			"[PointView:-directions_] Invalid dirArray argument."
			"Can be an Array of: Sphericals, or [[theta, phi], [theta, phi] ...], "
			"(rho assumed to be 1), or [[theta, phi, rho], [theta, phi, rho] ...]"
			.throw
		};

		this.points_(sphericals.collect(_.asCartesian));
	}


	drawFunc {
		^{ |v|
			var scale;
			var pnts, pnts_xf, pnt_depths;
			var axPnts, axPnts_xf, axPnts_depths;
			var rotPnts, to2D, incStep;
			var strRect;
			var minPntSize;
			var rrand;
			var variance;

			minPntSize = pointSize * pointDistScale;
			scale = minDim.half;

			rotPnts = { |carts|
				switch (rotateMode,
					\rtt, {
						carts.collect{ |pnt|
							pnt.rotate(rotate).tilt(tilt).tumble(tumble)
							.rotate(0.5pi).tilt(0.5pi) // orient so view matches ambisonics
						}
					},
					\ypr, {
						carts.collect{ |pnt|
							pnt
							.tilt(tilt).tumble(tumble).rotate(rotate)
							.rotate(0.5pi).tilt(0.5pi) // orient so view matches ambisonics
						}
					}
				)
			};

			// xformed points from 3D -> perspective -> 2D
			// + cart.z; accounts for depth adjusted by rotation
			// (az is the depth position of the _center_ of shape's rotation)
			// https://en.wikipedia.org/wiki/3D_projection
			to2D = { |carts|
				carts.collect{ |cart|
					(   cart
						+ (skewX @ skewY.neg)           // offset points within world, normalized
						* (bz / (az + cart.z))          // add perspective
						+ (translateX @ translateY.neg) // translate the "world"
					).asPoint * scale                   // discard z for 2D drawing, scale to window size
				}
			};

			variance = { |bool|
				if (bool) { 1 + rrand(randomVariance.neg, randomVariance) } { 1 }
			};

			// if rotating
			incStep = { |rand, curRot, step|
				(curRot + (step * variance.(rand))).wrap(-2pi, 2pi)
			};

			rotate = if (cycRotate) {
				incStep.(randomizedAxes.rotate, rotate, rotateStep)
			} { baseRotation };

			tilt = if (cycTilt) {
				incStep.(randomizedAxes.tilt,   tilt,   tiltStep)
			} { baseTilt };

			tumble = if (cycTumble) {
				incStep.(randomizedAxes.tumble, tumble, tumbleStep)
			} { baseTumble };

			// if oscillating
			if (oscRotate) {
				rotatePhase = ( // 0 to 2pi
					// rotatePhase + (rotateOscPhsInc * variance.(randomizedAxes.rotate) * rotateDir)
					rotatePhase + (rotateOscPhsInc * rotateDir)
				) % 2pi;
				rotate = sin(rotatePhase) * 0.5 * rotateOscWidth + baseRotation; //rotateOscCenter;
			};
			if (oscTilt) {
				tiltPhase = (
					// tiltPhase + (tiltOscPhsInc * variance.(randomizedAxes.tilt) * tiltDir)
					tiltPhase + (tiltOscPhsInc * tiltDir)
				) % 2pi;
				tilt = sin(tiltPhase) * 0.5 * tiltOscWidth + baseTilt; //tiltOscCenter;
			};
			if (oscTumble) {
				tumblePhase = (
					// tumblePhase + (tumbleOscPhsInc * variance.(randomizedAxes.tumble) * tumbleDir)
					tumblePhase + (tumbleOscPhsInc * tumbleDir)
				) % 2pi;
				tumble = sin(tumblePhase) * 0.5 * tumbleOscWidth + baseTumble; //tumbleOscCenter;
			};


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

				pntSize = pnt_depths[i].linlin(-1.0,1.0, pointSize, pointSize * pointDistScale);

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
				// Pen.fillColor_(Color.hsv(i / (pnts_xf.size - 1), 1, 1));
				Pen.fillColor_(prPntDrawCols.wrapAt(i));
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


	/* Perspective controls */

	// skew/offset the points in the world (before perspective is added)
	skewX_ { |norm|
		skewX = norm;
		this.refresh;
		this.changed(\skewX, norm);
	}
	skewY_ { |norm|
		skewY = norm;
		this.refresh;
		this.changed(\skewY, norm);
	}

	// translate the world (after perspective is added)
	translateX_ { |norm|   // translateX: left -> right = -1 -> 1
		translateX = norm;
		this.refresh;
		this.changed(\translateX, norm);
	}
	translateY_ { |norm|   // translateY: bottom -> top = -1 -> 1
		translateY = norm;
		this.refresh;
		this.changed(\translateY, norm);
	}

	// distance of points' origin to screen
	originDist_ { |norm|
		az = bz + norm;
		this.refresh;
		this.changed(\originDist, norm);
	}
	// distance of eye to screen
	eyeDist_ { |norm|
		var temp = az - bz; // store origin offset
		bz = norm;
		az = bz + temp;
		this.refresh;
		this.changed(\eyeDist, norm);
	}

	pointSize_ { |px = 15|
		pointSize = px;
		this.refresh;
		this.changed(\pointSize, px);
	}

	pointDistScale_ { |norm = 0.333|
		pointDistScale = norm;
		this.refresh;
		this.changed(\pointDistScale, norm);
	}

	// axis: '+X', '-X', '+Y', '-Y', '+Z', '-Z'
	setOrtho_ { |axis|
		ortho = true;
		orthoAxis = axis;
		perspective = false;
	}

	setPerspective {
		perspective = true;
	}

	/* View movement controls */

	rotate_ { |radians|
		baseRotation = radians;
		cycRotate = false;
		this.refresh;
		this.changed(\rotate, radians);
	}
	tilt_ { |radians|
		baseTilt = radians;
		cycTilt = false;
		this.refresh;
		this.changed(\tilt, radians);
	}
	tumble_ { |radians|
		baseTumble = radians;
		cycTumble = false;
		this.refresh;
		this.changed(\tumble, radians);
	}

	// rotation direction: 1 ccw, -1 cw
	rotateDir_ { |dir|
		rotateDir = dir;
		rotateStep = (rotateRate / frameRate) * 2pi * rotateDir;
		this.changed(\rotateDir, rotateDir);
	}
	tiltDir_ { |dir|
		tiltDir = dir;
		tiltStep = (tiltRate / frameRate) * 2pi * tiltDir;
		this.changed(\tiltDir, tiltDir);
	}
	tumbleDir_ { |dir|
		tumbleDir = dir;
		tumbleStep = (tumbleRate / frameRate) * 2pi * tumbleDir;
		this.changed(\tumbleDir, tumbleDir);
	}
	allDir_ { |dir|
		this.rotateDir_(dir).tiltDir_(dir).tumbleDir_(dir);
	}

	rotateRate_ { |hz|
		rotateRate = hz;
		rotateStep = (rotateRate / frameRate) * 2pi * rotateDir;
		this.changed(\rate, \rotate, hz);
	}
	tiltRate_ { |hz|
		tiltRate = hz;
		tiltStep = (tiltRate / frameRate) * 2pi * tiltDir;
		this.changed(\rate, \tilt, hz);
	}
	tumbleRate_ { |hz|
		tumbleRate = hz;
		tumbleStep = (tumbleRate / frameRate) * 2pi * tumbleDir;
		this.changed(\rate, \tumble, hz);
	}
	allRate_ { |hz|
		this.rotateRate_(hz).tiltRate_(hz).tumbleRate_(hz);
	}

	rotatePeriod_ { |seconds| this.rotateRate_(seconds.reciprocal) }
	tiltPeriod_   { |seconds| this.tiltRate_(seconds.reciprocal) }
	tumblePeriod_ { |seconds| this.tumbleRate_(seconds.reciprocal) }
	allPeriod_    { |seconds|
		this.rotateRate_(seconds).tiltRate_(seconds).tumbleRate_(seconds)
	}

	rotatePeriod { ^rotateRate.reciprocal }
	tiltPeriod   { ^tiltRate.reciprocal }
	tumblePeriod { ^tumbleRate.reciprocal }

	rotateCyc_ { |bool|
		cycRotate = bool;
		bool.if{ oscRotate = false };
		this.prCheckAnimate(\auto, \rotate, bool);
	}
	tiltCyc_ { |bool|
		cycTilt = bool;
		bool.if{ oscTilt = false };
		this.prCheckAnimate(\auto, \tilt, bool);
	}
	tumbleCyc_ { |bool|
		cycTumble = bool;
		bool.if{ oscTumble = false };
		this.prCheckAnimate(\auto, \tumble, bool);
	}
	allCyc_ { |bool|
		this.rotateCyc_(bool).tiltCyc_(bool).tumbleCyc_(bool);
		this.changed(\allCyc, bool);
	}

	rotateOsc_ { |bool|
		oscRotate = bool;
		bool.if{ cycRotate = false };
		this.prCheckAnimate(\rotate, bool);
	}
	tiltOsc_ { |bool|
		oscTilt = bool;
		bool.if{ cycTilt = false };
		this.prCheckAnimate(\tilt, bool);
	}
	tumbleOsc_ { |bool|
		oscTumble = bool;
		bool.if{ cycTumble = false };
		this.prCheckAnimate(\tumble, bool);
	}
	allOsc_ { |bool|
		this.rotateOsc_(bool).tiltOsc_(bool).tumbleOsc_(bool);
		this.changed(\allOsc, bool);
	}

	prCheckAnimate { |which, bool|
		userView.animate_(
			[   cycRotate, cycTilt, cycTumble,
				oscRotate, oscTilt, oscTumble
			].any({ |bool| bool })
		);
		this.changed(\osc, which, bool);
	}


	rotateOscPeriod_ { |seconds|
		rotateOscT = seconds;
		rotateOscPhsInc = 2pi / (seconds * frameRate);
		if (randomizedAxes.rotate) {
			rotateOscPhsInc = rotateOscPhsInc * (1 + rrand(0.05, randomVariance))
		};
		this.changed(\oscRotatePeriod, seconds);
	}
	tiltOscPeriod_ { |seconds|
		tiltOscT = seconds;
		tiltOscPhsInc = 2pi / (seconds * frameRate);
		if (randomizedAxes.tilt) {
			tiltOscPhsInc = tiltOscPhsInc * (1 + rrand(0.05, randomVariance))
		};
		this.changed(\oscTiltPeriod, seconds);
	}
	tumbleOscPeriod_ { |seconds|
		tumbleOscT = seconds;
		tumbleOscPhsInc = 2pi / (seconds * frameRate);
		if (randomizedAxes.tumble) {
			tumbleOscPhsInc = tumbleOscPhsInc * (1 + rrand(0.05, randomVariance))
		};
		this.changed(\oscTumblePeriod, seconds);
	}
	allOscPeriod_ { |seconds|
		this.rotateOscPeriod_(seconds).tiltOscPeriod_(seconds).tumbleOscPeriod_(seconds);
	}

	rotateOscWidth_  { |widthRad|
		// var deltaFactor;
		// deltaFactor = widthRad / rotateOscWidth;
		rotateOscWidth = widthRad;
		this.changed(\oscRotateWidth, widthRad);
		// this.rotateOscPeriod_(rotateOscT * deltaFactor)
	}
	tiltOscWidth_  { |widthRad|
		// var deltaFactor;
		// deltaFactor = widthRad / tiltOscWidth;
		tiltOscWidth = widthRad;
		this.changed(\oscTiltWidth, widthRad);
		// this.tiltOscPeriod_(tiltOscT * deltaFactor)
	}
	tumbleOscWidth_  { |widthRad|
		// var deltaFactor;
		// deltaFactor = widthRad / tumbleOscWidth;
		tumbleOscWidth = widthRad;
		this.changed(\oscTumbleWidth, widthRad);
		// this.tumbleOscPeriod_(tumbleOscT * deltaFactor)
	}
	allOscWidth_ { |widthRad|
		this.rotateOscWidth_(widthRad).tiltOscWidth_(widthRad).tumbleOscWidth_(widthRad);
	}

	rotateMode_ { |rttOrYpr|
		rotateMode = rttOrYpr;
		this.changed(\rotateMode, rotateMode);
		this.refresh;
	}

	varyMotion_ { |axis, bool|
		randomizedAxes[axis] = bool;
		// update osc periods
		this.rotateOscPeriod_(rotateOscT);
		this.tiltOscPeriod_(tiltOscT);
		this.tumbleOscPeriod_(tumbleOscT);
	}

	/* Display controls */

	showIndices_ { |bool|
		showIndices = bool;
		this.changed(\showIndices, bool);
		this.refresh;
	}

	showAxes_ { |bool|
		showAxes = bool;
		this.changed(\showAxes, bool);
		this.refresh;
	}

	showConnections_ { |bool|
		showConnections = bool;
		this.changed(\showConnections, bool);
		this.refresh;
	}

	// draw lines between these indices of points
	// e.g. [[1,3],[0,5],[2,4]]
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

	frameRate_ { |hz|
		frameRate = hz;
		userView.frameRate_(hz);
		this.changed(\frameRate, hz);
		// update rotation oscillator's phase step
		this.rotateOscPeriod_(rotateOscT);
	}


	/* Point color controls */

	// arrayOfColors can be a Color, Array of Colors.
	// If (arrayOfColors.size != points.size), points will wrap through the
	// color array, or be grouped into each color if colorSets has been set
	pointColors_ { |arrayOfColors|

		if (arrayOfColors.isKindOf(Color)) {
			arrayOfColors = [arrayOfColors];
		};

		if (
			arrayOfColors.isKindOf(Array) and:
			{ arrayOfColors.every({ |elem| elem.isKindOf(Color) }) }
		) {
			pointColors = arrayOfColors;
			prPntDrawCols = points.size.collect(pointColors.wrapAt(_));
			this.refresh;
		} {
			"[PointView:-pointColors_] arrayOfColors argument is not a Color or Array of Colors".throw;
		};
		colsByHue = false;
	}

	hueRange_ { |hueLow = 0, hueHigh = 0.999, sat = 0.9, val = 1, alpha = 0.8, scramble = false|
		var size = points.size;

		prPntDrawCols = size.collect{ |i|
			Color.hsv(
				(i / (size - 1)).linlin(0, 0.999, hueLow, hueHigh),
				sat, val, alpha
			)
		};
		if (scramble) {
			prPntDrawCols = prPntDrawCols.scramble;
			huesScrambled = scramble;
		};
		colsByHue = true;
	}

	// Set groups of point indices which belong to each color in
	// pointColors array.
	// defaultColor is a Color for points not includes in arraysOfIndices
	colorGroups_ { |arraysOfIndices, defaultColor = (Color.black)|

		prPntDrawCols = points.size.collect{defaultColor};

		if (arraysOfIndices.rank == 1) {
			arraysOfIndices = [arraysOfIndices];
		};

		arraysOfIndices.do{ |group, grpIdx|
			group.do{ |pntIdx|
				prPntDrawCols[pntIdx] = pointColors.wrapAt(grpIdx)
			}
		};
		colsByHue = false;
		this.refresh;
	}

	// called when points are set
	prUpdateColors {
		var hues, sat, val, alpha;
		if (colsByHue) {
			hues = prPntDrawCols.collect(_.hue);
			sat = prPntDrawCols.first.sat;
			val = prPntDrawCols.first.val;
			alpha = prPntDrawCols.first.alpha;
			this.hueRange_(hues.minItem, hues.maxItem, sat, val, alpha, huesScrambled);
		};

		prPntDrawCols ?? {
			prPntDrawCols = points.size.collect(pointColors.wrapAt(_));
			^this
		};

		if (prPntDrawCols.size != points.size) {

		}
	}

	// for UI controls
	units_ { |radiansOrDegrees|
		this.changed(\units, radiansOrDegrees)
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
v.cycRotate = true;
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
v.cycRotate = true;
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
// - add -highlightConnections(connection array) to emphasize a connection set (such as those triangles containing a VBAP source)
*/