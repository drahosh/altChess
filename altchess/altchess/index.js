
var ws = new WebSocket("ws://127.0.0.1:8082/");
var board,
    game = new Chess(),
    statusEl = $('#status'),
    fenEl = $('#fen'),
    pgnEl = $('#pgn');
var mycolor;
var cfg = {
    draggable: true,
    position: 'start',
    onDragStart: onDragStart,
    onDrop: onDrop,
    onSnapEnd: onSnapEnd,
    onMouseoutSquare: onMouseoutSquare,
    onMouseoverSquare: onMouseoverSquare,
};
var inroom=true;
ws.onopen = function() {
    state="open";


};
var ingame=false;
var state;
ws.onmessage = function (evt) {
    var j = JSON.parse(evt.data);
    if (j.messageType == "roomList") {

        roomtable = document.getElementById("roomlist");
        if (roomtable.hasChildNodes()) {
            while (roomtable.firstChild) {
                roomtable.removeChild(roomtable.firstChild);

            }
        }


        for (var i = 0; i < j.numOfRooms; i++) {
            var x = roomtable.insertRow(0);
            var y = x.insertCell(0);

            var z = x.insertCell(1);
            z = document.createElement("BUTTON");
            z.addEventListener("click", function(){
                var text ='{"messageType":"joinRoom", "number": 0}';
                var obj = JSON.parse(text);
                var tmp= 0;
                for(;tmp<i-1;tmp++){
                    tmp=tmp;
                }
                obj.number=tmp;
                ws.send(JSON.stringify(obj));
            });
            n = document.createTextNode("join room" + i);
            z.appendChild(n);
            x.appendChild(z);
            y.innerHTML = j.playernames[i];


        }

    }
    if (j.messageType == "game") {
        ingame = true;

        var myNode = document.getElementById("roomlist");
        while (myNode.firstChild) {
            myNode.removeChild(myNode.firstChild);
        }
        myNode=document.getElementById('refresh');
        myNode.parentNode.removeChild(myNode);
        myNode=document.getElementById('create');
        myNode.parentNode.removeChild(myNode);
        //cfg.position= j.fen;
        board = ChessBoard('board', cfg);
        mycolor='w';
        if (!j.iswhite){
            mycolor='b';
            board.flip();

        }
        updateStatus();
        $(document).ready();
    }
    if(j.messageType == "move"){

        game.move({
            from: j.from,
            to: j.to,
            promotion: j.promotion // NOTE: always promote to a queen for example simplicity
        });
        var movestring= j.from+"-"+ j.to;
        board.move(movestring);
        updateStatus();
    }
    if(j.messageType == "notice"){

       alert(j.text);
    }
}
ws.onclose = function() {
    statusEl.html('game closed');

    alert("Closed!");
};

ws.onerror = function(err) {
    alert("Error: " + err);
};
var createRoom = function(){
    var text ='{"messageType":"createRoom"}'
    ws.send(text);
}
var sendmove = function(move){
    var text ='{"messageType":"move", "from":"tmp", "to":"tmp", "promotion":"q"}';
    var obj = JSON.parse(text);
    obj.from=move.from;
    obj.to=move.to;

    ws.send(JSON.stringify(obj));


}
var refresh=function(){
    var text ='{"messageType":"getRoomList"}'
    ws.send(text);
}
function validateForm() {
    var x = document.forms["myForm"]["fname"].value;
    if (x == null || x == "") {
        alert("Name must be filled out");
        return false;
    }
    var text ='{"messageType":"login", "name":"tmp"}';
    var obj = JSON.parse(text);
    obj.name=x;
    ws.send(JSON.stringify(obj));
    var f=document.forms["myForm"]["fname"];

    f.parentNode.removeChild(f);
    f=document.getElementById("formtext");
    f.parentNode.removeChild(f);
    f=document.getElementById("formbutton");;
    f.parentNode.removeChild(f);
    text ='{"messageType":"getRoomList"}';
    ws.send(text);
    return false;

}
// do not pick up pieces if the game is over
// only pick up pieces for the side to move
    var onDragStart = function(source, piece, position, orientation) {
        if (game.game_over() === true ||
            (game.turn() === 'w' && piece.search(/^b/) !== -1) ||
            (game.turn() === 'b' && piece.search(/^w/) !== -1)) {
            return false;
        }
    };

    var onDrop = function(source, target) {
        // see if the move is legal
        var move = game.move({
            from: source,
            to: target,
            promotion: 'q' // NOTE: always promote to a queen for example simplicity
        });

        // illegal move
        if (move === null) return 'snapback';
        if (game.turn()==mycolor){
            game.undo();
            return 'snapback';
        }
        sendmove(move);
        updateStatus();
        game.undo();
    };
    var removeGreySquares = function() {
        $('#board .square-55d63').css('background', '');
    };

    var greySquare = function(square) {
        var squareEl = $('#board .square-' + square);

        var background = '#a9a9a9';
        if (squareEl.hasClass('black-3c85d') === true) {
            background = '#696969';
        }

        squareEl.css('background', background);
    };

// update the board position after the piece snap
// for castling, en passant, pawn promotion
    var onSnapEnd = function() {
        board.position(game.fen());
    };
    var onMouseoverSquare = function(square, piece) {
        // get list of possible moves for this square

        var moves = game.moves({
            square: square,
            verbose: true
        });

        // exit if there are no moves available for this square
        //if (moves.length === 0) return;

        // highlight the square they moused over
        greySquare(square);

        // highlight the possible squares for this piece
        for (var i = 0; i < moves.length; i++) {
            greySquare(moves[i].to);
        }
    };
    var updateStatus = function() {
        var status = '';

        var moveColor = 'White';
        if (game.turn() === 'b') {
            moveColor = 'Black';
        }

        // checkmate?
        if (game.in_checkmate() === true) {
            alert('Game over, ' + moveColor + ' is in checkmate.');
        }

        // draw?
        else if (game.in_draw() === true) {
            alert('Game over, drawn position');
        }

        // game still on
        else {
            status = moveColor + ' to move';

            // check?
            if (game.in_check() === true) {
                status += ', ' + moveColor + ' is in check';
            }
        }
        statusEl.html(status);
        fenEl.html(game.fen());
        pgnEl.html(game.pgn());
        $(document).ready();
    };
    var onMouseoutSquare = function(square, piece) {
        removeGreySquares();
    };
    var cfg = {
        draggable: true,
        position: 'start',
        onDragStart: onDragStart,
        onDrop: onDrop,
        onSnapEnd: onSnapEnd,
        onMouseoutSquare: onMouseoutSquare,
        onMouseoverSquare: onMouseoverSquare,
    };