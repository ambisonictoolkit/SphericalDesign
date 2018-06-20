// superclass of TDesign, etc
SphDesign {
	var <>points;   // Points are Cartesians
	var initPoints;
	var <design;

	*new {
		^super.new
	}

	// support for creating a TDesign throuh SphDesign
	*newT { |nPnts, t|
		^super.new.initTDesign(nPnts, t)
	}

	initTDesign { |nPnts, t|
		design = TDesign(nPnts, t, 3);
	}

	rotate { |angle| this.prUpdateDesign(\rotate, angle) }
	tilt { |angle| this.prUpdateDesign(\tilt, angle) }
	tumble { |angle| this.prUpdateDesign(\tumble, angle) }

	// modify the design by performing method on all points
	prUpdateDesign { |method ...args|
		points = points.collect(_.perform(method, *args));
		this.changed(\points)
	}

	directions { ^points.collect(_.asSpherical) }

	numPoints { ^points.size }

	size { ^points.size }

	// reset points to position when first created
	reset { points = initPoints }

	prSaveInitState { initPoints = points }

	visualize {
		// TODO: controls:
		// - RTT controls
		// - auto RTT with range/rate params
		// - show axes
		// - view controls: eye distance (perspective)
		// - orthogonal views: looking +/- XYZ axes
	}

	// triplets of points forming triangular mesh, e.g. for VBAP
	triplets {

	}
}


TDesign : SphDesign {
	var <t, nPnts, dim;

	*new { |nPnts, t, dim = 3|
		^super.new.init(nPnts, t, dim);
	}

	init { |aNp, aT, aDim|
		var path, data;

		nPnts = aNp;
		t = aT;
		dim = aDim;

		TDesignLib.lib ?? {TDesignLib.initLib};

		// update instance vars in case not all are specified by *new
		// errors out if no match or multiple matches found
		#nPnts, t, dim = this.prFindDesignMatch;

		path = TDesignLib.path +/+ "des.%.%.%.txt".format(dim, nPnts, t);
		if (File.exists(path).not) {
			"No t-design file found at %".format(path).throw
		};

		data = FileReader.read(path);

		points = data.collect(_.asFloat).flat.clump(3).collect{ |xyz|
			Cartesian(*xyz)
		};

		this.prSaveInitState;
	}

	prFindDesignMatch {
		var matches, m;

		matches = TDesignLib.getDesign(nPnts, t, dim);

		case
		{ matches.size == 0 } {
			"[TDesign:-init] No t-designs found in TDesignLib.lib matching "
			"nPnts %, t %, dim %".format(nPnts, t, dim).throw
		}
		{ matches.size > 1 } {
			var e;
			e = Error(
				"[TDesign:-init] Multiple t-designs found, specify both 'nPnts' "
				"and 't' to return one result. Available designs:"
			);
			e.errorString.postln;
			matches.do(_.postln);
			e.throw;
		}
		{ m = matches[0] };

		// unpack the dictionary to set instance vars on return
		^[m[\nPnts], m[\t], m[\dim]]
	}
}

TDesignLib {
	classvar <lib;   // Array of designs, stored as Dictionaries
	classvar <>path;
	// TODO: resolve default path
	classvar <defaultPath = "/Users/admin/Library/Application Support/ATK/t-designs/";

	*initLib {
		var pn, dim, nPnts, t;

		this.path ?? {this.path = defaultPath};

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
			Error("[TDesignLib:*initLib] No file exists at path %".format(this.path)).throw;
		};

		lib = List();

		pn.filesDo({ |f|
			#dim, nPnts, t = f.fileNameWithoutExtension.drop(4).split($.).asInt;
			lib.add(
				Dictionary.newFrom([
					\dim, dim, \nPnts, nPnts, \t, t
				]);
			);
		});
	}


	// Download all of the t-designs.
	// NOTE: may not be suitable for Windows
	*downloadAll { |savePath, makeDir = false|
		var p = savePath ?? {defaultPath};

		if (File.exists(p).not) {
			if (makeDir) {
				File.mkdir(p)
			} {
				format(
					"[TDesignLib:*download] Save path doesn't exist. "
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
				).split($\n).collect({|me| me.drop(5).reverse.drop(4).reverse});

			};
		};

		res.do(_.postln);
	}

	// return an Array of designs matching the criteria
	*getDesign { |nPnts, t, dim = 3|

		lib ?? {this.initLib};

		^lib.select{ |item|
			var t1, t2, t3;
			t1 = (nPnts.isNil or: {item[\nPnts] == nPnts});
			t2 = t.isNil or: {item[\t] == t};
			t3 = item[\dim] == dim;
			t1 and: t2 and: t3;
		}
	}

}
