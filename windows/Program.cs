using System.Runtime.InteropServices;
using InTheHand.Net.Bluetooth;
using InTheHand.Net.Sockets;

internal static class Program
{
    private static readonly Guid ServiceId = new("00001101-0000-1000-8000-00805F9B34FB");

    private static void Main()
    {
        Console.Title = "ER SK GAUR Presentation Remote";
        Console.WriteLine("ER SK GAUR Presentation Remote — Windows receiver");
        Console.WriteLine("Pair your Android phone with this laptop first.");
        Console.WriteLine("Waiting for Bluetooth RFCOMM connection…");
        using var listener = new BluetoothListener(ServiceId);
        listener.Start();

        while (true)
        {
            BluetoothClient client;
            try { client = listener.AcceptBluetoothClient(); }
            catch (Exception ex) { Console.WriteLine($"Bluetooth listener error: {ex.Message}"); Thread.Sleep(1500); continue; }

            Console.WriteLine("Phone connected.");
            using (client)
            using (var stream = client.GetStream())
            using (var reader = new StreamReader(stream, System.Text.Encoding.UTF8, false, 4096, true))
            using (var writer = new StreamWriter(stream, new System.Text.UTF8Encoding(false), 4096, true) { AutoFlush = true })
            {
                writer.WriteLine("STATUS:Bluetooth connected");
                while (client.Connected)
                {
                    string? command;
                    try { command = reader.ReadLine(); } catch { break; }
                    if (command is null) break;
                    Console.WriteLine($"Command: {command}");
                    try
                    {
                        switch (command.Trim().ToUpperInvariant())
                        {
                            case "NEXT": SendPowerPointKey("RIGHT"); break;
                            case "PREVIOUS": SendPowerPointKey("LEFT"); break;
                            case "START": SendPowerPointKey("F5"); break;
                            case "END": SendPowerPointKey("ESC"); break;
                            case "BLACK": SendPowerPointKey("B"); break;
                            case "VOLUME_UP": SendVolume(true); break;
                            case "VOLUME_DOWN": SendVolume(false); break;
                            case "PREVIEW":
                                var image = ExportCurrentSlide();
                                if (image is null) writer.WriteLine("STATUS:Start a PowerPoint slide show to preview");
                                else writer.WriteLine("PREVIEW:" + Convert.ToBase64String(image));
                                break;
                            default: writer.WriteLine("STATUS:Unknown command"); break;
                        }
                        if (command.Trim().ToUpperInvariant() != "PREVIEW")
                            writer.WriteLine("STATUS:Command sent");
                    }
                    catch (Exception ex) { writer.WriteLine("STATUS:Error - " + ex.Message.Replace('\n', ' ')); }
                }
            }
            Console.WriteLine("Phone disconnected; waiting for reconnect…");
        }
    }

    private static void SendPowerPointKey(string key)
    {
        if (key == "F5")
        {
            try { dynamic app = GetPowerPointApplication(); app.ActivePresentation.SlideShowSettings.Run(); return; }
            catch { }
        }
        if (key == "ESC")
        {
            try
            {
                dynamic app = GetPowerPointApplication();
                if (app.SlideShowWindows.Count > 0) { app.SlideShowWindows[1].View.Exit(); return; }
            }
            catch { }
        }
        try
        {
            dynamic app = GetPowerPointApplication();
            if (app.SlideShowWindows.Count > 0)
            {
                dynamic shell = Activator.CreateInstance(Type.GetTypeFromProgID("WScript.Shell")!)!;
                shell.AppActivate("PowerPoint Slide Show");
                shell.SendKeys(key == "RIGHT" ? "{RIGHT}" : key == "LEFT" ? "{LEFT}" : key == "B" ? "b" : key);
                return;
            }
        }
        catch { }
        dynamic fallbackShell = Activator.CreateInstance(Type.GetTypeFromProgID("WScript.Shell")!)!;
        fallbackShell.SendKeys(key == "RIGHT" ? "{RIGHT}" : key == "LEFT" ? "{LEFT}" : key == "B" ? "b" : key);
    }

    private static byte[]? ExportCurrentSlide()
    {
        try
        {
            dynamic app = GetPowerPointApplication();
            if (app.SlideShowWindows.Count < 1) return null;
            int index = (int)app.SlideShowWindows[1].View.Slide.SlideIndex;
            dynamic presentation = app.ActivePresentation;
            string path = Path.Combine(Path.GetTempPath(), $"erskgaur-slide-{Guid.NewGuid():N}.png");
            presentation.Slides[index].Export(path, "PNG", 1280, 720);
            byte[] bytes = File.ReadAllBytes(path);
            File.Delete(path);
            return bytes;
        }
        catch { return null; }
    }

    private static void SendVolume(bool up)
    {
        byte key = (byte)(up ? 0xAF : 0xAE);
        keybd_event(key, 0, 0, UIntPtr.Zero);
        keybd_event(key, 0, 2, UIntPtr.Zero);
    }

    private static object GetPowerPointApplication()
    {
        var type = Type.GetTypeFromProgID("PowerPoint.Application")
            ?? throw new InvalidOperationException("Microsoft PowerPoint desktop is not installed.");
        Guid clsid = type.GUID;
        GetActiveObject(ref clsid, IntPtr.Zero, out object app);
        return app;
    }

    [DllImport("oleaut32.dll", PreserveSig = false)]
    [return: MarshalAs(UnmanagedType.Interface)]
    private static extern void GetActiveObject(ref Guid rclsid, IntPtr reserved,
        [MarshalAs(UnmanagedType.Interface)] out object ppunk);

    [DllImport("user32.dll")]
    private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);
}
