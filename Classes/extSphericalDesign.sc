/*
	The following methods extend the SphericalDesign class and are
	stored here to denote that these methods are copied,
	with some modification, from Scott Wilson's port
	(VBAPSpeakerArray) of Ville Pukki's VBAP library in PureData.
	The copied methods pertain to forming a triangulation of points
	which forms a mesh grid suitable for use with VBAP and used here
	to visualizate a point mesh. A future implementation could
	perform a convex hull operation if that's more efficient.
	- M. McCrea, DXARTS, University of Washington, mtm5[at]uw.edu


VBAP created by Ville Pukki
This version ported from ver 0.99 PD code by Scott Wilson
Development funded in part by the AHRC http://www.ahrc.ac.uk

Copyright

This software is being provided to you, the licensee, by Ville Pulkki,
under the following license. By obtaining, using and/or copying this
software, you agree that you have read, understood, and will comply
with these terms and conditions: Permission to use, copy, modify and
distribute, including the right to grant others rights to distribute
at any tier, this software and its documentation for any purpose and
without fee or royalty is hereby granted, provided that you agree to
comply with the following copyright notice and statements, including
the disclaimer, and that the same appear on ALL copies of the software
and documentation, including modifications that you make for internal
use or for distribution:

Written by Ville Pulkki 1999
Helsinki University of Technology
and
Unversity of California at Berkeley

*/

+ SphericalDesign {

	/*
	Selects the loudspeaker triplets, and
	calculates the inversion matrices for each selected triplet.
	A line (connection) is drawn between each loudspeaker. The lines
	denote the sides of the triangles. The triangles should not be
	intersecting. All crossing connections are searched and the
	longer connection is erased. This yields non-intesecting triangles,
	which can be used in panning.
	See theory in:
	Pulkki, V. Lokki, T. "Creating Auditory Displays with Multiple
	Loudspeakers Using VBAP: A Case Study with DIVA Project",
	International Conference on Auditory Displays -98.
	*/

	// Slight refactor of the original choose_ls_triplets method -mtm
	calcTriplets { |minSideLength = 0.01|
		var i1, j1, k1, m, li;
		var distance_table, table_size, connections;
		var distance, step, dict, numPnts;

		numPnts = this.numPoints;

		// warn of high calculation times
		if (numPnts > 49) {
			var pntEnv, sec;

			// rough graph of calc times
			pntEnv = Env(
				[0, 5,  8,  11, 18, 34,  68,  425],               // sec
				[   50, 61, 70, 80, 100, 120, 180].differentiate, // numPoints
				'lin'
			);
			sec = if (numPnts < 181) { pntEnv[numPnts].round(0.1) };

			(
				"Calculating point triangulation...\n"
				"This could take roughly % %\n"
			).postf(
				*if (sec.notNil) {
					if (sec < 91) {
						[sec, "sec"]
					} {
						[(sec / 60).round(0.01), "min"]
					}
				} {
					["> 7", "min!"]
				}
			);
		};

		connections = Array.fill(numPnts, { Array.newClear(numPnts) });

		// fill a table with all the vector angles between points
		if (vecAngTable.isNil) {
			vecAngTable = Array.fill(numPnts, { Array.newClear(numPnts) });
			numPnts.do{ |i|
				vecAngTable[i][i] = 0; // fill the diagonal
				for (i+1.0, numPnts - 1, { |j|
					var va = this.vec_angle(points[i], points[j]);
					// can look up the value with either point index first
					vecAngTable[i][j] = va;
					vecAngTable[j][i] = va;
				})
			};
		};

		triplets = nil;
		for (0.0, numPnts - 1, { |i|
			for (i+1.0, numPnts - 1, { |j|
				for (j+1.0, numPnts - 1, { |k|
					if (this.vol_p_side_lgth(i,j,k) > minSideLength, {
						connections[i][j] = 1;
						connections[j][i] = 1;
						connections[i][k] = 1;
						connections[k][i] = 1;
						connections[j][k] = 1;
						connections[k][j] = 1;
						triplets = triplets.add([i,j,k]);
					});
				});
			});
		});

		/* calculate distancies between all lss and sorting them */
		table_size = ((numPnts - 1) * (numPnts)) / 2;
		distance_table = Array.newClear((numPnts * (numPnts - 1)) / 2);
		step = 0;

		numPnts.do{ |i|
			for(i+1, numPnts - 1, { |j|
				if (connections[i][j] == 1) {
					dict = Dictionary();
					dict[\d] = vecAngTable[i][j].abs;
					dict[\i] = i;
					dict[\j] = j;
					distance_table[step] = dict;
					step = step + 1;
				} {
					// keeping track of table size independently
					// is for some reason faster
					table_size = table_size - 1;
				};
			});
		};

		// sort by distance
		distance_table = distance_table[..step-1]; // trim to only those which were valid
		distance_table.sortBy(\d);

		/* disconnecting connections which are crossing shorter ones,
		starting from shortest one and removing all that cross it,
		and proceeding to next shortest */
		table_size.do{ |i|
			var fst_ls, sec_ls;
			fst_ls = distance_table[i][\i];
			sec_ls = distance_table[i][\j];

			if (connections[fst_ls][sec_ls] == 1, {
				numPnts.do{ |j|
					for (j+1.0, numPnts - 1, { |k|
						if (
							(j != fst_ls) and: {
								(k != sec_ls) and: {
									(k != fst_ls) and: {
										(j != sec_ls)
									}
								}
							}
						) {
							if (this.lines_intersect(fst_ls, sec_ls, j, k), {
								connections[j][k] = 0;
								connections[k][j] = 0;
							})
						}
					})
				}
			})
		};

		/* remove triangles which had crossing sides
		with smaller triangles or include loudspeakers */
		triplets = triplets.reject({ |set, idx|
			var test;
			i1 = set[0];
			j1 = set[1];
			k1 = set[2];
			test = (
				(connections[i1][j1] == 0) or: {
					(connections[i1][k1] == 0) or: {
						(connections[j1][k1] == 0) or: {
							this.any_ls_inside_triplet(i1,j1,k1)
						}
					}
				}
			);
			test
		}).asInteger; // cast indices to ints

		this.changed(\triplets, true); // true: triplets have been set
	}

	vec_angle { |v1, v2|
		/* angle between two loudspeakers */
		var inner;
		inner = (
			(v1.x * v2.x) + (v1.y * v2.y) + (v1.z * v2.z)
		) / (
			this.vec_length(v1) * this.vec_length(v2)
		);
		if (inner > 1.0,  { inner = 1.0 });
		if (inner < -1.0, { inner = -1.0 });
		^abs(acos(inner));
	}

	vec_length { |v1|
		/* length of a vector */
		^(sqrt(v1.x.squared + v1.y.squared + v1.z.squared));
	}

	vec_prod {|v1, v2|
		/* vector dot product */
		^((v1.x * v2.x) + (v1.y * v2.y) + (v1.z * v2.z));
	}


	lines_intersect { |i, j, k, l|
		/* checks if two lines intersect on 3D sphere */
		var v1, v2, v3, neg_v3;
		var angle;
		var dist_ij,dist_kl,dist_iv3,dist_jv3,dist_inv3,dist_jnv3;
		var dist_kv3,dist_lv3,dist_knv3,dist_lnv3;

		v1 = this.unq_cross_prod(points[i], points[j]);
		v2 = this.unq_cross_prod(points[k], points[l]);
		v3 = this.unq_cross_prod(v1, v2);

		neg_v3 = Cartesian.new;
		neg_v3.x= 0.0 - v3.x;
		neg_v3.y= 0.0 - v3.y;
		neg_v3.z= 0.0 - v3.z;

		dist_ij   = vecAngTable[i][j];
		dist_kl   = vecAngTable[k][l];
		dist_iv3  = this.vec_angle(points[i], v3);
		dist_jv3  = this.vec_angle(v3, points[j]);
		dist_inv3 = this.vec_angle(points[i], neg_v3);
		dist_jnv3 = this.vec_angle(neg_v3, points[j]);
		dist_kv3  = this.vec_angle(points[k], v3);
		dist_lv3  = this.vec_angle(v3, points[l]);
		dist_knv3 = this.vec_angle(points[k], neg_v3);
		dist_lnv3 = this.vec_angle(neg_v3, points[l]);

		/* if one of loudspeakers is close to crossing point, don't do anything */
		if (
			(abs(dist_iv3) <= 0.01) or: {
				(abs(dist_jv3) <= 0.01) or: {
					(abs(dist_kv3) <= 0.01) or: {
						(abs(dist_lv3) <= 0.01) or: {
							(abs(dist_inv3) <= 0.01) or: {
								(abs(dist_jnv3) <= 0.01) or: {
									(abs(dist_knv3) <= 0.01) or: {
										(abs(dist_lnv3) <= 0.01)
			}}}}}}}
		) {^false};

		/* if crossing point is on line between both loudspeakers return 1 */
		if (
			(
				(abs(dist_ij - (dist_iv3 + dist_jv3)) <= 0.01 ) and: {
					abs(dist_kl - (dist_kv3 + dist_lv3))  <= 0.01
				}
			) or: {
				(abs(dist_ij - (dist_inv3 + dist_jnv3)) <= 0.01)  and: {
					abs(dist_kl - (dist_knv3 + dist_lnv3)) <= 0.01
				}
			}
		) { ^true } { ^false };
	}

	/* vector cross product */
	unq_cross_prod { |v1, v2|
		var length, result;

		result = Cartesian.new;
		result.x = (v1.y * v2.z ) - (v1.z * v2.y);
		result.y = (v1.z * v2.x ) - (v1.x * v2.z);
		result.z = (v1.x * v2.y ) - (v1.y * v2.x);

		length = this.vec_length(result);
		result.x = result.x / length;
		result.y = result.y / length;
		result.z = result.z / length;

		^result;
	}

	/* calculate volume of the parallelepiped defined by the loudspeaker
	direction vectors and divide it with total length of the triangle sides.
	This is used when removing too narrow triangles. */
	vol_p_side_lgth { |i, j, k|
		var volper, lgth;
		var xprod;

		xprod =  this.unq_cross_prod(points[i], points[j]);
		volper = this.vec_prod(xprod, points[k]).abs;
		lgth = vecAngTable[i][j].abs + vecAngTable[i][k].abs + vecAngTable[j][k].abs;

		^if (lgth > 0.00001) { (volper / lgth) } { 0.0 };
	}

	/* returns true if there is loudspeaker(s) inside given ls triplet */
	any_ls_inside_triplet { |a, b, c|
		var invdet, invdetneg, tmp;
		var any_ls_inside, this_inside;

		var invmx = Array.newClear(9);
		var lp1 =  points[a];
		var lp2 =  points[b];
		var lp3 =  points[c];
		var lp2ylp3z_m_lp2zlp3y = (lp2.y * lp3.z) - (lp2.z * lp3.y);
		var lp2xlp3z_m_lp2zlp3x = (lp2.x * lp3.z) - (lp2.z * lp3.x);
		var lp2xlp3y_m_lp2ylp3x = (lp2.x * lp3.y) - (lp2.y * lp3.x);

		/* matrix inversion */
		invdet = 1.0 / (
			lp1.x * lp2ylp3z_m_lp2zlp3y
			- (lp1.y * lp2xlp3z_m_lp2zlp3x)
			+ (lp1.z * lp2xlp3y_m_lp2ylp3x)
		);

		invdetneg = invdet.neg;
		invmx[0] = lp2ylp3z_m_lp2zlp3y * invdet;
		invmx[3] = ((lp1.y * lp3.z) - (lp1.z * lp3.y)) * invdetneg;
		invmx[6] = ((lp1.y * lp2.z) - (lp1.z * lp2.y)) * invdet;
		invmx[1] = lp2xlp3z_m_lp2zlp3x * invdet.neg;
		invmx[4] = ((lp1.x * lp3.z) - (lp1.z * lp3.x)) * invdet;
		invmx[7] = ((lp1.x * lp2.z) - (lp1.z * lp2.x)) * invdetneg;
		invmx[2] = lp2xlp3y_m_lp2ylp3x * invdet;
		invmx[5] = ((lp1.x * lp3.y) - (lp1.y * lp3.x)) * invdetneg;
		invmx[8] = ((lp1.x * lp2.y) - (lp1.y * lp2.x)) * invdet;

		any_ls_inside = false;
		for (0, this.numPoints - 1, { |i|
			if((i != a) and: {
				(i != b) and: {
					(i != c) }
			}) {
				this_inside = true;
				for (0, 2, { |j|
					tmp = points[i].x * invmx[0 + (j * 3)];
					tmp = points[i].y * invmx[1 + (j * 3)] + tmp;
					tmp = points[i].z * invmx[2 + (j * 3)] + tmp;
					if (tmp < -0.001) { this_inside = false };
				});
				if (this_inside) { any_ls_inside = true };
			};
		});

		^any_ls_inside;
	}
}
