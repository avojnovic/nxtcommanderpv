<%@ Page Language="C#" AutoEventWireup="true" CodeFile="MessagePage.aspx.cs" Inherits="MessagePage" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head id="Head1" runat="server">
    <title></title>
</head>
<body>
    <form id="form1" runat="server">
    <script type="text/javascript">

    var lastReadedCommand = '';
    
    function showAndroidToast(toast) 
    {
        Cmd.showToast(toast);
    }

    function up() 
    {
        send('up');
        //Cmd.up();
    }

    function left() 
    {
        send('left');
        //Cmd.left();
    }

    function right() 
    {
        send('right');
        //Cmd.right();
    }

    function down() 
    {
        //Cmd.down();
        send('down');
    }

    function stop() 
    {
        send('stop');
        //Cmd.stop();
    }

    function executeCommand(command) 
    {
        if(command == 'up')
        {
            Cmd.up();
        }
        if(command == 'stop')
        {
            Cmd.stop();
        }
        if(command == 'left' )
        {
            Cmd.left();
        }
        if(command == 'right' )
        {
            Cmd.right();
        }
        if(command == 'down' )
        {
            Cmd.down();
        }
    }

    function poll() 
    {
        var command = $get('ChatHistoryList').value;
        if(command != lastReadedCommand)
        {
            //alert(command);                
            lastReadedCommand = command;
            executeCommand(command);
        }   

        var count = <%= History.Count %>;
        ChatWebService.PollForNewMessages(count, successCallback, failCallback);
    }

    function successCallback(result) 
    {
        if(result == true)
        {
            __doPostBack('ChatHistoryList', '');                     
            
        }


        poll();
    }
    
    function failCallback() 
    {
        poll();
    }

//    function send() 
//    {
//        var message = $get('Message').value;
//        $get('Message').value = "";
//        ChatWebService.SendMessage(message);
//    }
    
    function send(command)
    {
        ChatWebService.SendMessage(command);
    } 

    window.onload = poll;
    </script>
    <asp:ScriptManager ID="ScriptManager1" runat="server">
        <Services>
            <asp:ServiceReference Path="~/ChatWebService.asmx" />
        </Services>
    </asp:ScriptManager>
    <div>
    <table>
        <tr>
            <td></td>
            <td><input type="button" value="  Up  " onClick="up()" /></td>
            <td></td>
        </tr>
        <tr>
            <td><input type="button" value="Left" onClick="left()" /></td>
            <td></td>
            <td><input type="button" value="Right" onClick="right()" /></td>
        </tr>
        <tr>
            <td></td>
            <td><input type="button" value="Down" onClick="down()" /></td>
            <td></td>
        </tr>
        <tr>
            <td colspan="3"></td>            
        </tr>
        <tr>
            <td colspan="3"><input type="button" value="Stop" onClick="stop()" 
                    style="width: 132px" /></td>            
        </tr>
        
    </table>

        <%--<input type="button" value="Say hello" onClick="showAndroidToast('Hello Android!')" />--%>
        
        <asp:UpdatePanel runat="server" ID="ChatUpdatePanel">
            <ContentTemplate>
                <asp:TextBox runat="server" ClientIDMode="Static" ID="ChatHistoryList" Rows="10" Width="100%" />
            </ContentTemplate>
        </asp:UpdatePanel>
        <br />
        <input id="Message" /><input type="button" value="Send" onclick="send()" />
    </div>
    </form>
</body>
</html>
