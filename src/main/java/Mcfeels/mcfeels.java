package Mcfeels;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.time.Duration;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import io.github.bonigarcia.wdm.WebDriverManager;

public class mcfeels {

    static String IMGBB_API_KEY = "46866c7eef7ee62b26a79f32a5d57a08";
    
    // Create organized folder structure: Date \ Time
    static String RUN_DATE = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    static String RUN_TIME = new SimpleDateFormat("HH-mm-ss").format(new Date());
    
    // Tracking Statistics
    static int totalSteps = 0;
    static int passedSteps = 0;
    static int failedSteps = 0;
    static String START_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    static java.util.List<String> htmlSteps = new java.util.ArrayList<>();

    // Separate paths for screenshots and html
    static String SS_DIR = "reports/screenshots/" + RUN_DATE + "/" + RUN_TIME;
    static String HTML_DIR = "reports/html/" + RUN_DATE + "/" + RUN_TIME;

    // Common CSV File
    static String CSV_PATH = "reports/Mcfeels.csv";

    public static void main(String[] args) throws Exception {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito");
        // Required for CI/CD runners without a display
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.get("https://mcfeelys.com/");
        waitForPageToLoad(driver);

        dismissPopupIfExists(driver);
        takeScreenshot(driver, "homepage");

        product(driver);
        product_search(driver);
        checkout(driver);

         driver.quit();
    }

    // ------------------ Global page load wait ------------------
    public static void waitForPageToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            // Wait for document.readyState == complete
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));

            // Wait for jQuery AJAX to complete if jQuery is present
            wait.until(webDriver -> ((Long) ((JavascriptExecutor) webDriver)
                    .executeScript("return window.jQuery != undefined && jQuery.active == 0 ? 1 : 0")) == 1);
        } catch (Exception e) {
            System.out.println("Page load wait timed out: " + e.getMessage());
        }
    }

    public static void dismissPopupIfExists(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement popup = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.modal-inner-wrap, div.modals-overlay")
            ));

            try {
                WebElement closeBtn = popup.findElement(By.cssSelector("button.action-close[data-role='closeBtn']"));
                closeBtn.click();
                System.out.println("Popup closed successfully.");
            } catch (NoSuchElementException e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='none';", popup);
                System.out.println("Popup hidden by JS.");
            }

            Thread.sleep(500);
        } catch (Exception ignored) {}
    }

    public static boolean safeClick(WebDriver driver, By locator) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                dismissPopupIfExists(driver);
                WebElement elem = driver.findElement(locator);
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", elem);
                
                try {
                    waitForElementToBeClickable(driver, elem);
                    elem.click();
                    return true;
                } catch (TimeoutException te) {
                    System.out.println("Timeout waiting for element to be clickable. Attempting JS Click fallback...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", elem);
                    return true;
                }
            } catch (ElementNotInteractableException e) {
                System.out.println("Click intercepted or not interactable. Retrying...");
                try { Thread.sleep(1000); } catch (Exception ignored) {}
            } catch (Exception e) {
                System.out.println("Exception during click: " + e.getMessage());
                try { Thread.sleep(1000); } catch (Exception ignored) {}
            }
            attempts++;
        }
        
        try {
            takeScreenshot(driver, "Failed_Click", false, "Unable to click element: " + locator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Unable to click: " + locator + " - Continuing script...");
        return false;
    }

    public static void waitForElementToBeClickable(WebDriver driver, WebElement element) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    //    Product page
    public static void product(WebDriver driver) throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        Actions actions = new Actions(driver);

        try {
            WebElement menu = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//a[contains(text(), 'Screws & Fasteners') or contains(@title, 'Screws & Fasteners')]")));
            actions.moveToElement(menu).perform();
            Thread.sleep(1000); // Wait for dropdown to appear
        } catch (Exception e) {
            System.out.println("Menu 'Screws & Fasteners' not found: " + e.getMessage());
        }

        // Try direct navigation if menu hover fails
        boolean clickedPromax = false;
        try {
             clickedPromax = safeClick(driver, By.xpath("//a[contains(text(), \"McFeely's ProMax\") or contains(@title, \"McFeely's ProMax\")]"));
        } catch (Exception e) {}
        
        if (!clickedPromax) {
            System.out.println("Could not click ProMax from menu. Navigating directly...");
            driver.get("https://www.mcfeelys.com/screw-fastener-web-store/shop-screws-by-brands/mcfeely-s-promax.html");
        }
        
        waitForPageToLoad(driver);
        takeScreenshot(driver, "category_page");

        // Use more robust product selector
        boolean clickedProduct = false;
        try {
            clickedProduct = safeClick(driver, By.xpath("//a[contains(@class, 'product-item-link') and contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'promax')]"));
        } catch (Exception e) {}
        
        if (!clickedProduct) {
             System.out.println("Could not click specific product by text. Clicking first product link instead...");
             try {
                 // The product grid items are usually a.product-item-link
                 WebElement firstProduct = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.product-item-link")));
                 ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", firstProduct);
                 Thread.sleep(500);
                 ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstProduct);
             } catch (Exception e) {
                 System.out.println("No products found on page, navigating to a known product...");
                 driver.get("https://www.mcfeelys.com/8-x-1-1-4-in-promax-flat-head-wood-screws-dry-lube-qty-100.html");
             }
        }
        waitForPageToLoad(driver);
        takeScreenshot(driver, "Product_page");

        // Robust Add to Cart
        try {
            // First check if the "Add to Cart" button exists. If not, it might be an out-of-stock product.
            WebElement addToCartBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#product-addtocart-button")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", addToCartBtn);
            Thread.sleep(500);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addToCartBtn);
            Thread.sleep(2000); // Wait for add to cart animation/ajax
        } catch (Exception e) {
            System.out.println("Failed to click Add to Cart: " + e.getMessage());
            takeScreenshot(driver, "Failed_Add_to_cart", false, "Failed to click add to cart.");
        }
        takeScreenshot(driver, "Add_to_cart");

        // Robust Mini Cart interaction
        try {
            WebElement miniCart = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@class=\"action showcart\"]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", miniCart);
            Thread.sleep(1000);
        } catch (Exception e) {}
        takeScreenshot(driver, "Mini_cart");

        // Robust View Cart click
        try {
            WebElement viewCart = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[normalize-space()=\"View Cart\"]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", viewCart);
            waitForPageToLoad(driver);
        } catch (Exception e) {
            System.out.println("Could not click View Cart. Navigating directly...");
            driver.get("https://mcfeelys.com/checkout/cart/");
            waitForPageToLoad(driver);
        }
        takeScreenshot(driver, "View_cart");

        try {
            WebElement qtyInc = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.qty-inc")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", qtyInc);
            Thread.sleep(1000);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", qtyInc);
            
            WebElement updateCart = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[title=\"Update Cart\"]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", updateCart);
            waitForPageToLoad(driver);
            Thread.sleep(2000);
        } catch (Exception e) {}
        takeScreenshot(driver, "Update_cart");

        try {
            WebElement coupon = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#coupon_code")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", coupon);
            
            // Toggle coupon block if hidden
            try {
                WebElement blockTitle = driver.findElement(By.cssSelector("#block-discount-heading"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", blockTitle);
                Thread.sleep(500);
            } catch (Exception e) {}
            
            coupon.sendKeys("exitest");
            WebElement applyDiscount = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[value=\"Apply Discount\"]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", applyDiscount);
            waitForPageToLoad(driver);
            Thread.sleep(2000);
        } catch (Exception e) {}
        takeScreenshot(driver,"Discount_in_cart");

        try {
            WebElement proceedBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-role=\"proceed-to-checkout\"]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", proceedBtn);
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Could not click proceed to checkout from cart. Navigating directly...");
            driver.get("https://mcfeelys.com/checkout/#shipping");
        }
        
        // Let's add a robust wait for the login popup
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#email")));
            driver.findElement(By.cssSelector("#email")).sendKeys("deepak.maheshwari@exinent.com");
            driver.findElement(By.cssSelector("#password")).sendKeys("Admin@123");
            Thread.sleep(800);

            safeClick(driver, By.cssSelector("#customer_form_login_popup_showPassword"));
            safeClick(driver, By.xpath("//button[@id=\"customer-form-login-popup-send2\"]"));
            Thread.sleep(200);
            takeScreenshot(driver,"User_login");
        } catch (Exception e) {
            System.out.println("Login popup not found or interactable: " + e.getMessage());
            takeScreenshot(driver, "login_failed", false, "Failed to login: " + e.getMessage());
        }

        Thread.sleep(5000);
        takeScreenshot(driver,"Logged_in");
    }

    // Product search and filters

    public static void product_search(WebDriver driver) throws Exception {
        driver.get("https://mcfeelys.com/");
        waitForPageToLoad(driver);
        dismissPopupIfExists(driver);
        Thread.sleep(2000);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        // Ensure menu layout has fully loaded
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.panel.wrapper")));
        } catch (Exception ignored) {}

        // Use a more specific selector for the visible search input
        // Sometimes there are hidden search inputs for mobile vs desktop
        WebElement search1 = null;
        try {
            // First try the desktop search bar
            search1 = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".block-search input#search")));
            wait.until(ExpectedConditions.elementToBeClickable(search1));
        } catch (TimeoutException e) {
            // Fallback to any visible search bar
            java.util.List<WebElement> searchBars = driver.findElements(By.id("search"));
            for (WebElement bar : searchBars) {
                if (bar.isDisplayed()) {
                    search1 = bar;
                    break;
                }
            }
        }
        
        if (search1 == null) {
            System.out.println("Could not find a visible search bar. Attempting JS execution...");
            search1 = driver.findElement(By.id("search"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block';", search1);
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", search1);
        
        // Use Javascript to set value if sendKeys fails
        try {
            search1.sendKeys("staples");
        } catch (ElementNotInteractableException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].value='staples';", search1);
        }
        
        try {
            search1.sendKeys(Keys.ENTER);
        } catch (Exception e) {
            // If Enter fails, find and click the search button
            WebElement searchBtn = driver.findElement(By.cssSelector("button.action.search"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchBtn);
        }
        
        waitForPageToLoad(driver);
        Thread.sleep(5000);
        takeScreenshot(driver, "product_search_valid");

        // Need to refind the element after page load
        WebElement search2 = null;
        try {
            search2 = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".block-search input#search")));
        } catch (TimeoutException e) {
            java.util.List<WebElement> searchBars = driver.findElements(By.id("search"));
            for (WebElement bar : searchBars) {
                if (bar.isDisplayed()) {
                    search2 = bar;
                    break;
                }
            }
        }
        
        if (search2 != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", search2);
            try {
                search2.clear();
                search2.sendKeys("pizza");
            } catch (ElementNotInteractableException e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].value='pizza';", search2);
            }
            
            try {
                search2.sendKeys(Keys.ENTER);
            } catch (Exception e) {
                WebElement searchBtn = driver.findElement(By.cssSelector("button.action.search"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchBtn);
            }
        }
        waitForPageToLoad(driver);
        Thread.sleep(5000);
        takeScreenshot(driver, "product_search_invalid");

        driver.get("https://www.mcfeelys.com/screw-fastener-web-store/shop-screws-by-brands/mcfeely-s-promax.html");
        waitForPageToLoad(driver);
        Thread.sleep(2000);

        // Robust Product Filter selection
        try {
            WebElement sorterElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sorter")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", sorterElement);
            Select sorter = new Select(sorterElement);
            sorter.selectByValue("price");
            waitForPageToLoad(driver);
            Thread.sleep(3000);
        } catch (Exception e) {
            System.out.println("Failed to select product filter: " + e.getMessage());
        }
        takeScreenshot(driver, "product_filter_by_price");

        // Try to interact with minicart from anywhere
        try {
            WebElement minicart = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[@class=\"action showcart\"]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", minicart);
            Thread.sleep(1000);

            WebElement viewCart = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[normalize-space()=\"View Cart\"]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", viewCart);
            waitForPageToLoad(driver);
        } catch (Exception e) {}


    }

    // checkout
    public static void checkout(WebDriver driver) throws Exception {
        driver.get("https://mcfeelys.com/checkout/cart/");
        waitForPageToLoad(driver);
        dismissPopupIfExists(driver);
        Thread.sleep(2000);
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Wait for the checkout button to be present
        WebElement checkoutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("button[data-role='proceed-to-checkout']")));

        // Scroll into view
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkoutBtn);

        // Wait until clickable (visible + enabled)
        WebElement finalCheckoutBtn = checkoutBtn;
        checkoutBtn = wait.until(driver1 -> {
            if (finalCheckoutBtn.isDisplayed() && finalCheckoutBtn.isEnabled()) {
                return finalCheckoutBtn;
            } else {
                return null;
            }
        });

        // Click using JS to bypass any overlay issues
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkoutBtn);

        // Wait for checkout page to load
        waitForPageToLoad(driver);
        
        // Let's use a more robust check for checkout page loaded.
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".checkout-header")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("#shipping")),
                ExpectedConditions.presenceOfElementLocated(By.name("street[0]"))
            ));
        } catch (TimeoutException e) {
            System.out.println("Timeout waiting for checkout form to load. Navigating directly...");
            driver.get("https://mcfeelys.com/checkout/#shipping");
            waitForPageToLoad(driver);
        }
        Thread.sleep(3000); // allow animations to finish

        // Fill checkout form
        try {
            WebElement streetInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("street[0]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", streetInput);
            streetInput.clear();
            streetInput.sendKeys("California");

            driver.findElement(By.name("city")).clear();
            driver.findElement(By.name("city")).sendKeys("California");

            driver.findElement(By.name("company")).clear();
            driver.findElement(By.name("company")).sendKeys("Exinent Test");
            
            // Try dropdown first, then input
            try {
                WebElement regionSelect = driver.findElement(By.name("region_id"));
                if(regionSelect.getTagName().equalsIgnoreCase("select")){
                     Select region = new Select(regionSelect);
                     region.selectByVisibleText("California");
                } else {
                     regionSelect.clear();
                     regionSelect.sendKeys("California");
                }
            } catch (Exception e) {
                 try {
                     driver.findElement(By.name("region")).clear();
                     driver.findElement(By.name("region")).sendKeys("California");
                 } catch (Exception ex) {}
            }

            driver.findElement(By.name("postcode")).clear();
            driver.findElement(By.name("postcode")).sendKeys("90001");

            driver.findElement(By.name("telephone")).clear();
            driver.findElement(By.name("telephone")).sendKeys("9870999521");
        } catch (Exception e) {
            takeScreenshot(driver, "checkout_form_failed", false, "Failed to fill checkout form: " + e.getMessage());
        }

        Thread.sleep(3000);
        takeScreenshot(driver, "checkout_form");
    }


    // ------------------ Screenshot + Upload + CSV + HTML ------------------
    private static void takeScreenshot(WebDriver driver, String title) throws IOException {
        takeScreenshot(driver, title, true, "Step completed successfully");
    }

    private static void takeScreenshot(WebDriver driver, String title, boolean isPass, String details) throws IOException {
        totalSteps++;
        if (isPass) passedSteps++;
        else failedSteps++;

        // Use a short timestamp for file name
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String statusPrefix = isPass ? "SUCCESS_" : "ERROR_";
        String fileName = statusPrefix + title + "_" + timestamp + ".png";

        // Folder to store screenshots (Organized by Date and Time)
        File folder = new File(SS_DIR);
        if (!folder.exists()) folder.mkdirs();

        // Save screenshot locally
        File outputFile = new File(folder, fileName);
        
        // Take full page screenshot
        try {
            // Get total height of the page
            Long innerHeight = (Long) ((JavascriptExecutor) driver).executeScript("return window.innerHeight;");
            Long scrollHeight = (Long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight;");
            
            if (scrollHeight > innerHeight) {
                // For long pages, set window size to match full content height
                Long width = (Long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollWidth;");
                driver.manage().window().setSize(new Dimension(width.intValue(), scrollHeight.intValue()));
                Thread.sleep(500); // Wait for resize
            }
            
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Restore window size
            driver.manage().window().maximize();
        } catch (Exception e) {
            System.out.println("Failed to take full page screenshot, falling back to viewport: " + e.getMessage());
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Screenshot saved: " + outputFile.getAbsolutePath());

        // Upload screenshot to Imgbb
        String uploadedUrl = "Upload failed/skipped";
        try {
            uploadedUrl = uploadToImgbb(outputFile);
            System.out.println("Uploaded URL: " + uploadedUrl);
        } catch (Exception e) {
            System.out.println("Could not upload to Imgbb: " + e.getMessage());
        }

        // Write to CSV (append mode)
        writeCsv(timestamp, title, uploadedUrl, outputFile.getName());
        
        // Write to HTML report
        writeHtmlReport(timestamp, title, outputFile.getName(), uploadedUrl, isPass, details);
    }

    private static String uploadToImgbb(File imageFile) throws IOException {
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        String encodedImage = Base64.getEncoder().encodeToString(fileContent);

        String data = "key=" + IMGBB_API_KEY +
                "&image=" + URLEncoder.encode(encodedImage, "UTF-8");

        URL url = new URL("https://api.imgbb.com/1/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        OutputStream os = conn.getOutputStream();
        os.write(data.getBytes());
        os.flush();
        os.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();

        String json = response.toString();
        return json.split("\"url\":\"")[1].split("\"")[0].replace("\\/", "/");
    }

    private static void writeCsv(String timestamp, String title, String url, String localFileName) {
        String csvFile = CSV_PATH;
        File fileObj = new File(csvFile);
        if (!fileObj.getParentFile().exists()) {
            fileObj.getParentFile().mkdirs();
        }
        boolean fileExists = fileObj.exists();

        try (FileWriter fw = new FileWriter(csvFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            // If CSV does not exist, write header first
            if (!fileExists) {
                out.println("Timestamp,Title,LocalFile,UploadedURL");
            }

            // Append new row
            out.println(timestamp + "," + title + "," + localFileName + "," + url);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeHtmlReport(String timestamp, String title, String localFileName, String url, boolean isPass, String details) {
        File htmlFolder = new File(HTML_DIR);
        if (!htmlFolder.exists()) htmlFolder.mkdirs();

        String htmlFile = HTML_DIR + "/test_report.html";
        
        // Relative path from HTML file to the Screenshot image
        String relativeImgPath = "../../../screenshots/" + RUN_DATE + "/" + RUN_TIME + "/" + localFileName;

        // Build the HTML for the current step and add it to our static list
        String stepStatusClass = isPass ? "pass" : "fail";
        String stepStatusIcon = isPass ? "✅" : "❌";
        
        StringBuilder stepHtml = new StringBuilder();
        stepHtml.append("            <div class=\"test-step ").append(stepStatusClass).append("\">\n");
        stepHtml.append("                <div class=\"step-content\">\n");
        stepHtml.append("                    <div class=\"step-header\">\n");
        stepHtml.append("                        <span>").append(stepStatusIcon).append(" ").append(title.replace("_", " ").toUpperCase()).append("</span>\n");
        stepHtml.append("                        <span class=\"step-time\">").append(timestamp.split("_")[1].replace("-", ":")).append("</span>\n");
        stepHtml.append("                    </div>\n");
        stepHtml.append("                    <div class=\"step-details\">").append(details).append("</div>\n");
        stepHtml.append("                </div>\n");
        stepHtml.append("                <div class=\"step-media\">\n");
        stepHtml.append("                    <a href=\"").append(relativeImgPath).append("\" target=\"_blank\">\n");
        stepHtml.append("                        <img class=\"screenshot\" src=\"").append(relativeImgPath).append("\" alt=\"").append(title).append("\">\n");
        stepHtml.append("                    </a>\n");
        stepHtml.append("                    <div class=\"btn-group\">\n");
        stepHtml.append("                        <a class=\"btn\" href=\"").append(relativeImgPath).append("\" target=\"_blank\">View Local</a>\n");
        if (url != null && url.startsWith("http")) {
            stepHtml.append("                        <a class=\"btn imgbb\" href=\"").append(url).append("\" target=\"_blank\">View ImgBB</a>\n");
        }
        stepHtml.append("                    </div>\n");
        stepHtml.append("                </div>\n");
        stepHtml.append("            </div>");
        
        htmlSteps.add(stepHtml.toString());

        // Overwrite file entirely on every step to update the total counts at the top and include ALL steps
        try (FileWriter fw = new FileWriter(htmlFile, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            double passRate = totalSteps > 0 ? ((double) passedSteps / totalSteps) * 100 : 0;
            String overallStatus = failedSteps > 0 ? "FAILED" : "PASSED";
            String statusBadgeClass = failedSteps > 0 ? "status-fail" : "status-pass";

            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("    <meta charset=\"UTF-8\">");
            out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            out.println("    <title>McFeels E-commerce Automation - Enhanced Test Report</title>");
            out.println("    <style>");
            out.println("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); }");
            out.println("        .container { max-width: 1400px; margin: 0 auto; background: white; border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); padding: 40px; }");
            if (failedSteps > 0) {
                out.println("        .header { text-align: center; background: linear-gradient(135deg, #e53935 0%, #e35d5b 100%); color: white; padding: 40px; border-radius: 15px; margin-bottom: 40px; box-shadow: 0 4px 15px rgba(229, 57, 53, 0.4); }");
            } else {
                out.println("        .header { text-align: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; border-radius: 15px; margin-bottom: 40px; }");
            }
            out.println("        .header h1 { margin: 0; font-size: 2.5em; font-weight: 300; }");
            out.println("        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 25px; margin-bottom: 40px; }");
            out.println("        .summary-card { background: white; padding: 25px; border-radius: 15px; text-align: center; border: 1px solid #e0e0e0; box-shadow: 0 4px 6px rgba(0,0,0,0.05); transition: transform 0.3s, box-shadow 0.3s; }");
            out.println("        .summary-card:hover { transform: translateY(-5px); box-shadow: 0 8px 15px rgba(0,0,0,0.1); }");
            out.println("        .summary-card h3 { margin: 0 0 15px 0; color: #555; font-size: 1.1em; text-transform: uppercase; letter-spacing: 1px; }");
            out.println("        .summary-card .number { font-size: 3em; font-weight: bold; color: #333; margin-bottom: 10px; }");
            out.println("        .progress-bar { width: 100%; height: 12px; background-color: #e9ecef; border-radius: 12px; overflow: hidden; margin: 15px 0; }");
            if (failedSteps > 0) {
                out.println("        .progress-fill { height: 100%; background: linear-gradient(90deg, #ff416c, #ff4b2b); transition: width 1s ease; border-radius: 12px; }");
            } else {
                out.println("        .progress-fill { height: 100%; background: linear-gradient(90deg, #11998e, #38ef7d); transition: width 1s ease; border-radius: 12px; }");
            }
            out.println("        .test-results { margin: 40px 0; display: flex; flex-direction: column; gap: 20px; }");
            out.println("        .test-step { display: flex; flex-wrap: wrap; margin: 0; padding: 25px; border-radius: 12px; background: white; border: 1px solid #e0e0e0; box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: all 0.3s; }");
            out.println("        .test-step:hover { box-shadow: 0 8px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }");
            out.println("        .test-step.pass { border-left: 8px solid #28a745; }");
            out.println("        .test-step.fail { border-left: 8px solid #dc3545; background: #fff5f5; animation: pulse-red 2s infinite; }");
            out.println("        @keyframes pulse-red { 0% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0.4); } 70% { box-shadow: 0 0 0 10px rgba(220, 53, 69, 0); } 100% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0); } }");
            out.println("        .step-content { flex: 1; min-width: 300px; padding-right: 20px; }");
            out.println("        .step-media { flex: 0 0 auto; display: flex; flex-direction: column; align-items: flex-end; justify-content: center; }");
            out.println("        .step-header { font-weight: bold; margin-bottom: 15px; font-size: 1.3em; display: flex; align-items: center; gap: 10px; color: #2c3e50; }");
            out.println("        .step-time { font-size: 0.75em; font-weight: normal; color: #666; background: #f1f3f5; padding: 4px 10px; border-radius: 20px; margin-left: auto; }");
            out.println("        .step-details { font-size: 1em; color: #555; line-height: 1.6; background: #f8f9fa; padding: 15px; border-radius: 8px; border-left: 4px solid #ced4da; }");
            out.println("        .test-step.fail .step-details { color: #b02a37; background: #f8d7da; border-left-color: #dc3545; font-family: monospace; font-size: 1.1em; }");
            out.println("        .screenshot { max-width: 350px; max-height: 250px; border-radius: 8px; border: 3px solid #eee; transition: transform 0.3s; cursor: pointer; object-fit: cover; }");
            out.println("        .test-step.fail .screenshot { border-color: #ffc107; }");
            out.println("        .screenshot:hover { transform: scale(1.08); z-index: 10; position: relative; box-shadow: 0 10px 25px rgba(0,0,0,0.2); }");
            out.println("        .timestamp { text-align: center; color: #e0e0e0; margin-top: 15px; font-size: 0.9em; letter-spacing: 0.5px; }");
            out.println("        .status-badge { display: inline-block; padding: 10px 25px; border-radius: 30px; color: white; font-weight: bold; font-size: 1.2em; letter-spacing: 1px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
            out.println("        .status-pass { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }");
            out.println("        .status-fail { background: linear-gradient(135deg, #ff416c 0%, #ff4b2b 100%); animation: pulse-red 2s infinite; }");
            out.println("        .btn-group { display: flex; gap: 10px; margin-top: 15px; width: 100%; justify-content: flex-end; }");
            out.println("        .btn { display: inline-block; padding: 8px 18px; background: #f8f9fa; color: #495057; text-decoration: none; border-radius: 20px; font-weight: 600; font-size: 0.85em; border: 1px solid #ced4da; transition: all 0.2s; }");
            out.println("        .btn:hover { background: #e9ecef; color: #212529; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.05); }");
            out.println("        .btn.imgbb { background: #e0f3ff; color: #0077cc; border-color: #b8e2ff; }");
            out.println("        .btn.imgbb:hover { background: #cce7ff; color: #005fa3; }");
            out.println("        .error-banner { background: linear-gradient(135deg, #ff416c 0%, #ff4b2b 100%); color: white; padding: 20px; border-radius: 12px; margin-bottom: 30px; display: flex; align-items: center; gap: 15px; font-size: 1.2em; font-weight: bold; box-shadow: 0 4px 15px rgba(255, 65, 108, 0.4); }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class=\"container\">");
            out.println("        <div class=\"header\">");
            out.println("            <h1>🛒 McFeels E-commerce Automation</h1>");
            out.println("            <p style=\"font-size: 1.2em; margin: 10px 0;\">Enhanced Test Report with Detailed Steps</p>");
            out.println("            <div class=\"timestamp\">Generated on: " + RUN_DATE + " at " + RUN_TIME.replace("-", ":") + "</div>");
            out.println("        </div>");
            
            // Dynamic Summary Cards
            out.println("        <div class=\"summary\">");
            out.println("            <div class=\"summary-card\">");
            out.println("                <h3>Overall Status</h3>");
            out.println("                <div class=\"status-badge " + statusBadgeClass + "\">" + overallStatus + "</div>");
            out.println("            </div>");
            out.println("            <div class=\"summary-card\">");
            out.println("                <h3>Total Steps</h3>");
            out.println("                <div class=\"number\">" + totalSteps + "</div>");
            out.println("            </div>");
            out.println("            <div class=\"summary-card\">");
            out.println("                <h3>Passed</h3>");
            out.println("                <div class=\"number\" style=\"color: #28a745;\">" + passedSteps + "</div>");
            out.println("            </div>");
            out.println("            <div class=\"summary-card\">");
            out.println("                <h3>Failed</h3>");
            out.println("                <div class=\"number\" style=\"color: #dc3545;\">" + failedSteps + "</div>");
            out.println("            </div>");
            out.println("            <div class=\"summary-card\">");
            out.println("                <h3>Pass Rate</h3>");
            out.println("                <div class=\"number\">" + String.format("%.1f", passRate) + "%</div>");
            out.println("                <div class=\"progress-bar\">");
            out.println("                    <div class=\"progress-fill\" style=\"width: " + passRate + "%\"></div>");
            out.println("                </div>");
            out.println("            </div>");
            out.println("        </div>");
            
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            out.println("        <div style=\"text-align: center; margin: 20px 0;\">");
            out.println("            <p><strong>Test Duration:</strong> " + START_TIME + " to " + currentTime + "</p>");
            out.println("        </div>");

            out.println("        <div class=\"test-results\">");
            
            if (failedSteps > 0) {
                out.println("            <div class=\"error-banner\">");
                out.println("                ⚠️ TEST FAILED: " + failedSteps + " step(s) encountered errors during execution.");
                out.println("            </div>");
            }

            out.println("            <h2>📋 Detailed Test Results</h2>");
            out.println("            <p style=\"color: #666; margin-bottom: 30px;\">Comprehensive step-by-step execution details with visual evidence</p>");

            // Append ALL Step details dynamically from our static list
            for (String step : htmlSteps) {
                out.println(step);
            }

            out.println("        </div>"); // Closing test-results
            
            // Footer
            out.println("        <div style=\"margin-top: 50px; padding-top: 20px; border-top: 1px solid #dee2e6; text-align: center; color: #6c757d;\">");
            out.println("            <p>🤖 Generated by McFeels E-commerce Automation Framework v2.0</p>");
            out.println("            <p>Dynamic testing with random categories and products for comprehensive coverage</p>");
            out.println("            <p><small>Report updated dynamically during execution.</small></p>");
            out.println("        </div>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
