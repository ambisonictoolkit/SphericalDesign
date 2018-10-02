/*
	Copyright the ATK Community and Joseph Anderson, 2011-2017
        J Anderson	j.anderson[at]ambisonictoolkit.net
        M McCrea    mtm5[at]uw.edu

	This file is part of a Spherical Design library for SuperCollider3.
	This is free software:
	you can redistribute it and/or modify it under the terms of the GNU General
	Public License as published by the Free Software Foundation, either version 3
	of the License, or (at your option) any later version.

	The SphericalDesign library is distributed in
	the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
	implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
	the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along with
	this library. If not, see <http://www.gnu.org/licenses/>.
*/


//-------------------------------------------------------------------------
// Third Party Notices
//-------------------------------------------------------------------------
//
//-------------------------------------------------------------------------
// This library includes an extension, found in extSphDesign.sc, containing
// methods which ar a modification of Scott Wilson's SC port of Ville
// Pukki's VBAP in PD. Wilson's comments and Pulkki's copyright statement
// can be found in that file.
//-------------------------------------------------------------------------
//
//-------------------------------------------------------------------------
// The T-Designs found here are from the work of:
//    McLaren's Improved Snub Cube and Other New Spherical Designs in Three
//    Dimensions, R. H. Hardin and N. J. A. Sloane, Discrete and Computational
//    Geometry, 15 (1996), pp. 429-441.
// and are downloaded directly from their site:
//    http://neilsloane.com/sphdesigns/
//-------------------------------------------------------------------------


SphericalDesign {
	var <points;     // points are Cartesians
	var initPoints;
	var <view;
	var <triplets;   // delaunay triangulation triplets, see extSphericalDesign
	var vecAngTable; // used by calcTriplets, see extSphericalDesign

	*new {
		^super.new
	}

	// support for creating a TDesign via SphDesign
	*newT { |numPoints, t|
		^TDesign(numPoints, t, 3);
	}

	// transform the design

	// rotations - angle in radians
	rotate { |angle| this.performOnDesign(\rotate, angle) }
	tilt   { |angle| this.performOnDesign(\tilt, angle) }
	tumble { |angle| this.performOnDesign(\tumble, angle) }

	// mirror
	mirrorX { |recalcTriplets = true| // reflecting across the YZ plane
		this.prMirror(\mirrorX, recalcTriplets) }
	mirrorY { |recalcTriplets = true| // reflecting across the XZ plane
		this.prMirror(\mirrorY, recalcTriplets) }
	mirrorZ { |recalcTriplets = true| // reflecting across the XY plane
		this.prMirror(\mirrorZ, recalcTriplets) }
	mirrorO { |recalcTriplets = true| // reflecting through the origin
		this.prMirror(\mirrorO, recalcTriplets) }

	prMirror { |method, recalcTriplets|
		this.performOnDesign(method);
		// only recalculate if triplets were previously calculated
		if (recalcTriplets and: { triplets.notNil }) { this.calcTriplets }
	}

	// modify the design by performing method on all points (Cartesians)
	// leaves triplets unchanged
	performOnDesign { |methodOrFunc ... args|
		case
		{ methodOrFunc.isKindOf(Symbol) } {
			this.points_(
				points.collect(_.perform(methodOrFunc, *args))
			)
		}
		{ methodOrFunc.isKindOf(Function) } {
			this.points_(
				points.collect({ |pnt, i| methodOrFunc.value(pnt, i, *args) })
			)
		}
	}

	numPoints { ^points.size }
	size { ^points.size }

	// reset points to position when first created, e.g. after applying rotation
	reset {
		this.points_(initPoints.deepCopy);
		this.resetTriplets;
	}

	points_ { |cartesianArray|
		points = cartesianArray;
		this.changed(\points);
	}

	// azElArray: 2D array containing [azimuth, inclination] (theta, phi) pairs
	directions_ { |azElArray|
		this.points_(
			azElArray.collect{ |dirs| Spherical(1, *dirs).asCartesian}
		);
	}

	// return a 2D array containing [azimuth, inclination] (theta, phi) pairs
	directions {
		^points.collect({ |cart|
			var sph = cart.asSpherical;
			[sph.theta, sph.phi]
		})
	}

	// vector angles from all points to a given direction
	vectorAngles { |theta = 0, phi = 0|
		^points.collect{ |point| this.vec_angle(Spherical(1, theta, phi), point) }
	}

	nearestIndex { |theta = 0, phi = 0|
		^this.vectorAngles(theta, phi).minIndex
	}

	nearestPoint { |theta = 0, phi = 0|
		^points[this.nearestIndex(theta, phi)]
	}

	indicesWithin { |theta = 0, phi = 0, spread = 0.5pi, inclusive = true|
		var lessThan, thresh;

		lessThan = if (inclusive) { '<=' } { '<' };
		thresh = spread.half;

		^this.vectorAngles(theta, phi).selectIndices({ |angle|
			angle.perform(lessThan, thresh)
		})
	}

	pointsWithin { |theta = 0, phi = 0, spread = 0.5pi, inclusive = true|
		^this.points.at(
			this.indicesWithin(theta, phi, spread, inclusive)
		)
	}

	// like indicesWithin, but indices are sorted from nearest to farthest point
	nearestIndicesWithin { |theta = 0, phi = 0, spread = 0.5pi, inclusive = true|
		var lessThan, thresh, vecAngles, sortIndices, ret;
		var farthestIndex = 0;

		lessThan = if (inclusive) { '<=' } { '<' };
		thresh = spread.half;

		vecAngles = this.vectorAngles(theta, phi);
		sortIndices = vecAngles.order;

		ret = [];

		while ({ vecAngles[sortIndices[farthestIndex]].perform(lessThan, thresh) }, {
			ret = ret.add(sortIndices[farthestIndex]);
			farthestIndex = farthestIndex + 1;
		});

		^ret
	}

	// like nearestIndicesWithin, but indices are sorted from nearest to farthest point
	nearestPointsWithin { |theta = 0, phi = 0, spread = 0.5pi, inclusive = true|
		^this.points.at(
			this.nearestIndicesWithin(theta, phi, spread, inclusive)
		)
	}

	// showConnections will draw triangular connections between points
	visualize { |parent, bounds, showConnections = true|
		if (showConnections and: { triplets.isNil }) {
			"\nTriangulating points to display connections...".postln;
			try { this.calcTriplets } { triplets = nil; "Could not calculate triplets".warn };
			"...done".postln;
		};

		view = PointView(parent, bounds).points_(points).front;
		this.addDependant(view);
		view.onClose_({ view.onClose.addFunc({ this.removeDependant(view) }) });

		if (showConnections and: { triplets.notNil }) {
			view.connectTriplets_(triplets);
		};
	}

	resetTriplets {
		triplets = nil;
		vecAngTable = nil;
		this.changed(\triplets, false); // false: triplets have not been set
	}

	captureState { initPoints = points.deepCopy }
}


TDesign : SphericalDesign {
	var <t;

	*new { |numPoints, t, dim = 3|
		^super.new.init(numPoints, t, dim);
	}

	init { |argNp, argT, argDim|
		var path, data, matches;

		// confirm one and only one match exists for this numPoints and t
		// errors out if no match or multiple matches found
		matches = TDesignLib.getDesign(argNp, argT, argDim);
		case
		{ matches.size == 0 } {
			Error(
				format("[TDesign:-init] No t-designs found in TDesignLib.lib matching: "
				"numPoints %, t %, dim %", argNp, argT, argDim)
			).throw;
		}
		{ matches.size > 1 } {
			var e;
			e = Error(
				"[TDesign:-init] Multiple t-designs found, specify both 'numPoints' "
				"and 't' to return one result. 't' of available designs:"
			);
			e.errorString.postln;
			matches.do({ |design| postf("t: %\n", design[\t]) });
			e.throw;
		};

		t = matches[0][\t];

		path = TDesignLib.path +/+ "des.%.%.%.txt".format(argDim, argNp, t);
		if (File.exists(path).not) {
			format("No t-design file found at %", path).throw
		};

		data = FileReader.read(path);

		points = data.collect(_.asFloat).flat.clump(3).collect{ |xyz|
			Cartesian(*xyz)
		};

		this.captureState;
	}
}

// A class to download, import, sort and retrieve T-Designs.
// http://neilsloane.com/sphdesigns/
TDesignLib {
	classvar <lib;   // Array of designs, stored as Dictionaries
	classvar >path;
	classvar <defaultPath;

	*initClass {
		defaultPath = PathName(this.filenameSymbol.asString).parentPath;
		defaultPath = PathName(defaultPath).parentPath +/+ "Designs/t-designs/sloane/";
	}

	*initLib {
		var pn, dim, nPnts, t;

		path ?? { path = defaultPath.standardizePath };

		if (File.exists(path)) {
			pn = PathName(path);
			if (pn.files.size == 0) {
				Error(
					format(
						"[TDesignLib:*initLib] No t-design files found at %\n"
						"Set TDesignLib.path to the location of your t-design files, \n"
						"or use TDesignLib.downloadAll to download them if you don't yet have them.",
						pn.fullPath
					)
				).throw;
			}
		} {
			Error("[TDesignLib:*initLib] No folder exists at path %".format(this.path)).throw;
		};

		lib = List();

		pn.filesDo({ |f|
			#dim, nPnts, t = f.fileNameWithoutExtension.drop(4).split($.).asInt;
			lib.add(
				Dictionary.newFrom([
					\dim, dim, \numPoints, nPnts, \t, t
				]);
			);
		});
	}


	// Download all of the t-designs.
	// NOTE: uses curl, may not be suitable for Windows
	*downloadAll { |savePath, makeDir = false|
		var p;

		p = savePath ?? {defaultPath};

		if (File.exists(p).not) {
			if (makeDir) {
				File.mkdir(p)
			} {
				format(
					"[TDesignLib:*downloadAll] Save path doesn't exist. "
					"Set makeDir=true to create it. [%]",
					path
				).throw
			}
		};

		// parse filenames from t-design repository and iteratively download each
		postf("Downloading t-designs to %\nPlease wait ...", p);
		unixCmd(
			format(
				"curl -s http://neilsloane.com/sphdesigns/dim3/ | "
				"grep href | grep \".txt\" | sed 's/.*href=\"//' | sed 's/\".*//' | "
				"while read -r fname; do curl -o %$fname -f http://neilsloane.com/sphdesigns/dim3/$fname; done",
				p.asCompileString
			),
			action: { |...args|
				if (args[0] != 0) {
					"Could not download t-designs from http://neilsloane.com/sphdesigns/dim3/".throw;
				};
				this.path = p;
				this.initLib;
				"Done downloading t-designs.".postln
			},
			postOutput: true
		);
	}

	// post all designs, and if none found locally,
	// retrieve available designs online
	*availableDesigns {
		var res;

		// check locally for loaded designs
		if (lib.notNil) {
			res = lib
		} {
			try {
				this.initLib;
				res = lib;
			} { |error|
				// couldn't load the library
				error.errorString.warn;

				"No local designs are available, but the following "
				"can be downloaded by calling *download.".postln;

				// report what's available online
				res = unixCmdGetStdOut(
					format(
						"curl -s http://neilsloane.com/sphdesigns/dim3/ | "
						"grep href | grep \".txt\" | sed 's/.*href=\"//' | sed 's/\".*//'",
						this.path.asCompileString
					)
				).split($\n).collect({ |me| me.drop(5).reverse.drop(4).reverse });

			};
		};

		res.sortBy(\numPoints).do({ |d| postf("numPoints -> %, t -> %\n", d[\numPoints], d[\t]) });
	}

	// return an Array of designs matching the criteria
	*getDesign { |numPoints, t, dim = 3|

		lib ?? {this.initLib};

		^lib.select{ |item|
			var t1, t2, t3;
			t1 = (numPoints.isNil or: { item[\numPoints] == numPoints });
			t2 = t.isNil or: { item[\t] == t };
			t3 = item[\dim] == dim;
			t1 and: t2 and: t3;
		}
	}

	*path { ^path ?? defaultPath }
}
