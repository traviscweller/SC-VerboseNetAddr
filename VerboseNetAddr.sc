VerboseNetAddr : NetAddr {
	var <addr=0, <>port, <hostname, <socket;
	classvar connections;

	*initClass {
		connections = IdentityDictionary.new;
	}

	*new { arg hostname, port;
		var addr;
		addr = if (hostname.notNil,{ hostname.gethostbyname },{ 0 });
		^super.newCopyArgs(addr, port, hostname);
	}

	*fromIP { arg addr, port;
		^super.newCopyArgs(addr, port, addr.asIPString)
	}

	*langPort {
		_GetLangPort
		^this.primitiveFailed;
	}

	*matchLangIP {|ipstring|
		_MatchLangIP
		^this.primitiveFailed;
	}

	*localEndPoint {
		^this.new(this.langIP, this.langPort)
	}

	*localAddr {
		^this.new("127.0.0.1", this.langPort)
	}

	*useDoubles_ { arg flag = false;
		_NetAddr_UseDoubles
		^this.primitiveFailed;
	}

	*broadcastFlag {
		_NetAddr_GetBroadcastFlag
		^this.primitiveFailed
	}

	*broadcastFlag_ { arg flag = true;
		_NetAddr_SetBroadcastFlag
		^this.primitiveFailed
	}

	*disconnectAll {
		if(connections.notNil) {
			connections.keys.do { |netAddr|
				netAddr.disconnect;
			}
		}
	}

	*connections {
		^connections.copy;
	}

	hostname_ { arg inHostname;
		hostname = inHostname;
		addr = inHostname.gethostbyname;
	}

	sendRaw { arg rawArray;
		_NetAddr_SendRaw
		^this.primitiveFailed;
	}

	/* ======= BEGIN MODIFICATIONS ======== */

	sendMsg { |... args|
		var cmd = args[0];
		if(cmd.isKindOf(Integer)) {
			args[0] = #[
				"/none",
				"/notify",
				"/status",
				"/quit",
				"/cmd",
				"/d_recv",
				"/d_load",
				"/d_loadDir",
				"/d_freeAll",
				"/s_new",
				"/n_trace",
				"/n_free",
				"/n_run",
				"/n_cmd",
				"/n_map",
				"/n_set",
				"/n_setn",
				"/n_fill",
				"/n_before",
				"/n_after",
				"/u_cmd",
				"/g_new",
				"/g_head",
				"/g_tail",
				"/g_freeAll",
				"/c_set",
				"/c_setn",
				"/c_fill",
				"/b_alloc",
				"/b_allocRead",
				"/b_read",
				"/b_write",
				"/b_free",
				"/b_close",
				"/b_zero",
				"/b_set",
				"/b_setn",
				"/b_fill",
				"/b_gen",
				"/dumpOSC",
				"/c_get",
				"/c_getn",
				"/b_get",
				"/b_getn",
				"/s_get",
				"/s_getn",
				"/n_query",
				"/b_query",
				"/n_mapn",
				"/s_noid",
				"/g_deepFree",
				"/clearSched",
				"/sync",
				"/d_free",
				"/b_allocReadChannel",
				"/b_readChannel",
				"/g_dumpTree",
				"/g_queryTree",
				"/error",
				"/s_newargs",
				"/n_mapa",
				"/n_mapan",
				"/n_order",
				"/p_new",
				"/version"][cmd] // fill this list with all the commands from the command reference
		};
		this.superPerformList(\sendMsg, args)
	}

	sendBundle { arg time ... args;
		var cmd = args[0];
		if(cmd.isKindOf(Integer)) {
			args[0] = #[
				"/none",
				"/notify",
				"/status",
				"/quit",
				"/cmd",
				"/d_recv",
				"/d_load",
				"/d_loadDir",
				"/d_freeAll",
				"/s_new",
				"/n_trace",
				"/n_free",
				"/n_run",
				"/n_cmd",
				"/n_map",
				"/n_set",
				"/n_setn",
				"/n_fill",
				"/n_before",
				"/n_after",
				"/u_cmd",
				"/g_new",
				"/g_head",
				"/g_tail",
				"/g_freeAll",
				"/c_set",
				"/c_setn",
				"/c_fill",
				"/b_alloc",
				"/b_allocRead",
				"/b_read",
				"/b_write",
				"/b_free",
				"/b_close",
				"/b_zero",
				"/b_set",
				"/b_setn",
				"/b_fill",
				"/b_gen",
				"/dumpOSC",
				"/c_get",
				"/c_getn",
				"/b_get",
				"/b_getn",
				"/s_get",
				"/s_getn",
				"/n_query",
				"/b_query",
				"/n_mapn",
				"/s_noid",
				"/g_deepFree",
				"/clearSched",
				"/sync",
				"/d_free",
				"/b_allocReadChannel",
				"/b_readChannel",
				"/g_dumpTree",
				"/g_queryTree",
				"/error",
				"/s_newargs",
				"/n_mapa",
				"/n_mapan",
				"/n_order",
				"/p_new",
				"/version"][cmd] // fill this list with all the commands from the command reference
		};
		this.superPerformList(\sendBundle, args)
	}

	/* ======= END MODIFICATIONS ======== */

	sendStatusMsg {
		this.sendMsg("/status");
	}

	sendClumpedBundles { arg time ... args;
		if(args.bundleSize > 65535) {// udp max size.
			args.clumpBundles.do { |item|
				if(time.notNil) { time = time + 1e-9 }; // make it a nanosecond later
				this.sendBundle(time, *item)
			};
		} {
			this.sendBundle(time, *args)
		}
	}

	sync { arg condition, bundles, latency; // array of bundles that cause async action
		var resp, id;
		if (condition.isNil) { condition = Condition.new };
		if(bundles.isNil) {
			id = this.makeSyncResponder(condition);
			this.sendBundle(latency, ["/sync", id]);
			condition.wait;
		} {
			// not sure what the exact size is, but its about 20000
			// this relates to what _NetAddr_SendBundle can send
			if(bundles.bundleSize > 20000/*65515*/) { // 65515 = 65535 - 16 - 4 (sync msg size)
				bundles.clumpBundles.do { |item|
					id = this.makeSyncResponder(condition);
					this.sendBundle(latency, *(item ++ [["/sync", id]]));
					if(latency.notNil) { latency = latency + 1e-9 };
					condition.wait;
				}
			} {
				id = this.makeSyncResponder(condition);
				this.sendBundle(latency, *(bundles ++ [["/sync", id]]));
				condition.wait;
			}
		};
		// maybe needed: a timeout
	}

	makeSyncResponder { arg condition;
		var id = UniqueID.next;
		var resp = OSCFunc({|msg|
			if (msg[1] == id) {
				resp.free;
				condition.test = true;
				condition.signal;
			};
		}, '/synced', this);
		condition.test = false;
		^id
	}

	isConnected {
		^socket.notNil
	}

	connect { | disconnectHandler |
		if (this.isConnected.not) {
			this.prConnect;
			connections.put(this, disconnectHandler ? {});
		}
	}

	disconnect {
		if (this.isConnected) {
			this.prDisconnect;
			this.prConnectionClosed;
		};
	}

	tryConnectTCP { |onComplete, onFailure, maxAttempts = 10|
		var func = { |attempts|
			attempts = attempts - 1;
			try {
				this.connect
			} { |err|
				if(err.isKindOf(PrimitiveFailedError) and: { err.failedPrimitiveName == '_NetAddr_Connect'}) {
					if(attempts > 0) {
						0.2.wait;
						func.value(attempts)
					} {
						"Couldn't connect to TCP address %:%\n".format(hostname, port).warn;
						onFailure.value(this)
					}
				} {
					err.throw;
				}
			}
		};
		fork {
			func.value(maxAttempts);
			onComplete.value(this);
		}
	}

	tryDisconnectTCP { |onComplete, onFailure|
		try {
			this.disconnect
		} { |err|
			if(err.isKindOf(PrimitiveFailedError) and: { err.failedPrimitiveName == '_NetAddr_SendMsg'}) {
				"Couldn't disconnect from TCP address %:%\n".format(hostname, port).warn;
				onFailure.value(this)
			} {
				err.throw;
			}
		};
		this.disconnect;
		onComplete.value(this);
	}

	== { arg that;
		^this.compareObject(that, #[\port, \addr])
	}

	hash {
		^this.instVarHash(#[\port, \addr])
	}

	// Asymmetric: "that" may be nil or have nil port (wildcards)
	matches { arg that;
		^that.isNil
		or: { this.isLocal and: { that.isLocal } and: { that.port.isNil or: { this.port == that.port } } }
		or: { this == that }
		or: { that.port.isNil and: { this.addr == that.addr } }
	}

	isLocal { ^this.class.matchLangIP(this.ip) }

	ip {
		^addr.asIPString
	}

	hasBundle { ^false }

	printOn { | stream |
		super.printOn(stream);
		stream << $( << this.ip << ", " << port << $)
	}
	storeOn { | stream |
		super.storeOn(stream);
		stream << $( << "\"" << this.ip << "\", " << port << $)
	}

	// PRIVATE
	prConnect {
		_NetAddr_Connect
		^this.primitiveFailed;
	}

	prDisconnect {
		_NetAddr_Disconnect
		^this.primitiveFailed;
	}

	prConnectionClosed {
		// called when connection is closed either by sclang or by peer
		socket = nil;
		connections.removeAt(this).value(this);
	}

	recover { ^this }
}
