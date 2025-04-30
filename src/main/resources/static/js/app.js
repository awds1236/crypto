/**
 * 가상화폐 AI 분석 서비스 JavaScript
 * 업비트 API 데이터와 Claude AI를 이용한 가상화폐 분석 시스템
 */

// 전역 변수
let priceChart = null;
let rsiChart = null;
let stompClient = null;
let currentMarket = null;
let realTimePrices = {};

// DOM이 로드되면 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 코인 목록 로드
    loadMarkets();
    
    // 분석 버튼 이벤트
    document.getElementById('analyzeBtn').addEventListener('click', function() {
        const market = document.getElementById('marketSelect').value;
        if (market) {
            currentMarket = market;
            analyzeMarket(market);
            
            // 알림 권한 요청 (선택 사항)
            requestNotificationPermission();
        } else {
            alert('코인을 선택해주세요');
        }
    });
});

/**
 * 업비트 API에서 시장 데이터 로드
 */
function loadMarkets() {
    fetch('/markets')
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            const select = document.getElementById('marketSelect');
            
            // KRW 마켓만 필터링
            const krwMarkets = data.filter(market => market.market.startsWith('KRW-'));
            
            // 시가총액 순 정렬 (정확한 정렬을 위해서는 추가 API 호출 필요)
            krwMarkets.sort((a, b) => a.korean_name.localeCompare(b.korean_name));
            
            krwMarkets.forEach(market => {
                const option = document.createElement('option');
                option.value = market.market;
                option.textContent = `${market.korean_name} (${market.market})`;
                select.appendChild(option);
            });
            
            // BTC 자동 선택 (선택 사항)
            const btcOption = Array.from(select.options).find(option => option.value === 'KRW-BTC');
            if (btcOption) {
                btcOption.selected = true;
            }
        })
        .catch(error => {
            console.error('코인 목록 로드 실패:', error);
            showError('코인 목록을 불러오는 데 실패했습니다. 페이지를 새로고침 해주세요.');
        });
}

/**
 * 선택한 마켓에 대한 분석 실행
 * @param {string} market - 마켓 코드 (예: KRW-BTC)
 */
function analyzeMarket(market) {
    // 로딩 표시
    document.getElementById('loading').style.display = 'block';
    document.getElementById('analysisResult').style.display = 'none';
    
    // 이전 WebSocket 연결 종료
    if (stompClient) {
        stompClient.disconnect();
    }
    
    fetch(`/analyze?market=${market}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            // 로딩 숨김
            document.getElementById('loading').style.display = 'none';
            
            if (data.success) {
                // 결과 표시 애니메이션
                const resultElement = document.getElementById('analysisResult');
                resultElement.classList.add('fade-in');
                resultElement.style.display = 'block';
                
                // 텍스트 분석 결과
                document.getElementById('analysisText').innerHTML = formatAnalysisText(data.analysis);
                
                // 기술적 지표
                displayTechnicalIndicators(data.indicators.latest);
                
                // 공포/욕심 지수
                displayFearGreedIndex(data.fearGreedIndex);
                
                // 뉴스 표시
                displayNews(data.news);
                
                // 차트 그리기
                drawPriceChart(data.indicators);
                drawRsiChart(data.indicators.rsi14);
                
                // 실시간 데이터 연결
                stompClient = connectWebSocket(market);
                
                // 타이틀 업데이트
                updatePageTitle(market);
            } else {
                showError('분석 중 오류가 발생했습니다: ' + data.error);
            }
        })
        .catch(error => {
            document.getElementById('loading').style.display = 'none';
            console.error('분석 실패:', error);
            showError('서버 통신 중 오류가 발생했습니다');
        });
}

/**
 * 분석 텍스트 포맷팅
 * @param {string} analysis - 분석 결과 텍스트
 * @return {string} 포맷팅된 HTML
 */
function formatAnalysisText(analysis) {
    // 줄바꿈과 마크다운 형식을 HTML로 변환
    return analysis
        .replace(/\n\n/g, '<br><br>')
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/# (.*?)$/gm, '<h3>$1</h3>')
        .replace(/## (.*?)$/gm, '<h4>$1</h4>')
        .replace(/### (.*?)$/gm, '<h5>$1</h5>');
}

/**
 * 기술적 지표 표시
 * @param {Object} indicators - 기술적 지표 객체
 */
function displayTechnicalIndicators(indicators) {
    const container = document.getElementById('technicalIndicators');
    container.innerHTML = '';
    
    const table = document.createElement('table');
    table.className = 'table table-sm';
    
    Object.entries(indicators).forEach(([key, value]) => {
        const row = table.insertRow();
        
        const keyCell = row.insertCell();
        keyCell.textContent = formatIndicatorName(key);
        keyCell.className = 'indicator-label';
        
        const valueCell = row.insertCell();
        valueCell.textContent = parseFloat(value).toFixed(2);
        valueCell.className = 'indicator-value';
        
        // RSI에 따른 색상 지정
        if (key === 'rsi14') {
            if (value > 70) {
                valueCell.className += ' text-danger'; // 과매수
            } else if (value < 30) {
                valueCell.className += ' text-success'; // 과매도
            }
        }
    });
    
    container.appendChild(table);
}

/**
 * 지표 이름 포맷팅
 * @param {string} key - 지표 키
 * @return {string} 포맷팅된 지표 이름
 */
function formatIndicatorName(key) {
    const names = {
        'sma20': 'SMA (20)',
        'ema20': 'EMA (20)',
        'rsi14': 'RSI (14)'
    };
    
    return names[key] || key;
}

/**
 * 공포/욕심 지수 표시
 * @param {Object} data - 공포/욕심 지수 데이터
 */
function displayFearGreedIndex(data) {
    const container = document.getElementById('fearGreedIndex');
    container.innerHTML = '';
    
    const value = data.value;
    const classification = data.valueClassification;
    
    const valueElement = document.createElement('div');
    valueElement.className = 'fear-greed-value mb-2';
    valueElement.textContent = value;
    
    const classElement = document.createElement('div');
    classElement.className = 'mb-1';
    classElement.textContent = classification;
    
    // 지수에 따른 색상 지정
    if (value < 25) {
        valueElement.className += ' extreme-fear'; // 극도의 공포
    } else if (value < 40) {
        valueElement.className += ' fear'; // 공포
    } else if (value < 60) {
        valueElement.className += ' neutral'; // 중립
    } else if (value < 80) {
        valueElement.className += ' greed'; // 욕심
    } else {
        valueElement.className += ' extreme-greed'; // 극도의 욕심
    }
    
    container.appendChild(valueElement);
    container.appendChild(classElement);
    
    // 시각적 게이지 추가
    const gauge = document.createElement('div');
    gauge.className = 'progress';
    gauge.style.height = '20px';
    
    const bar = document.createElement('div');
    bar.className = 'progress-bar';
    bar.style.width = value + '%';
    
    // 게이지 색상
    if (value < 25) {
        bar.className += ' bg-danger';
    } else if (value < 40) {
        bar.className += ' bg-warning';
    } else if (value < 60) {
        bar.className += ' bg-info';
    } else if (value < 80) {
        bar.className += ' bg-primary';
    } else {
        bar.className += ' bg-success';
    }
    
    gauge.appendChild(bar);
    container.appendChild(gauge);
}

/**
 * 뉴스 데이터 표시
 * @param {Object} newsData - 뉴스 데이터 객체
 */
function displayNews(newsData) {
    const container = document.getElementById('newsContainer');
    container.innerHTML = '';
    
    if (!newsData || !newsData.news || newsData.news.length === 0) {
        container.innerHTML = '<p class="text-center">관련 뉴스가 없습니다.</p>';
        return;
    }
    
    const newsList = document.createElement('div');
    newsList.className = 'list-group';
    
    newsData.news.slice(0, 5).forEach(news => {
        const newsItem = document.createElement('div');
        newsItem.className = 'news-item';
        
        const title = document.createElement('div');
        title.className = 'news-title';
        title.textContent = news.title;
        
        const date = document.createElement('div');
        date.className = 'news-date';
        const publishedAt = new Date(news.published_on * 1000);
        date.textContent = publishedAt.toLocaleString();
        
        const url = document.createElement('a');
        url.href = news.url;
        url.target = '_blank';
        url.className = 'btn btn-sm btn-outline-primary mt-2';
        url.textContent = '뉴스 보기';
        
        newsItem.appendChild(title);
        newsItem.appendChild(date);
        newsItem.appendChild(url);
        
        newsList.appendChild(newsItem);
    });
    
    container.appendChild(newsList);
}

/**
 * 가격 차트 그리기
 * @param {Object} data - 차트 데이터
 */
function drawPriceChart(data) {
    const ctx = document.getElementById('priceChart').getContext('2d');
    
    // 기존 차트 제거
    if (priceChart) {
        priceChart.destroy();
    }
    
    // 데이터 준비
    const sma20 = data.sma20;
    const ema20 = data.ema20;
    
    // 차트 생성
    priceChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: Array.from({length: sma20.length}, (_, i) => i + 1),
            datasets: [
                {
                    label: 'SMA (20)',
                    data: sma20,
                    borderColor: 'rgba(54, 162, 235, 1)',
                    backgroundColor: 'rgba(54, 162, 235, 0.1)',
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: 'EMA (20)',
                    data: ema20,
                    borderColor: 'rgba(255, 99, 132, 1)',
                    backgroundColor: 'rgba(255, 99, 132, 0.1)',
                    borderWidth: 2,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    display: true,
                    title: {
                        display: true,
                        text: '일자'
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: true,
                        text: '가격'
                    }
                }
            },
            plugins: {
                tooltip: {
                    mode: 'index',
                    intersect: false,
                },
                legend: {
                    position: 'top',
                },
                title: {
                    display: true,
                    text: '가격 및 지표 추세'
                }
            },
            interaction: {
                mode: 'nearest',
                axis: 'x',
                intersect: false
            },
            elements: {
                line: {
                    tension: 0.4 // 부드러운 곡선
                }
            }
        }
    });
}

/**
 * RSI 차트 그리기
 * @param {Array} rsiData - RSI 데이터 배열
 */
function drawRsiChart(rsiData) {
    const ctx = document.getElementById('rsiChart').getContext('2d');
    
    // 기존 차트 제거
    if (rsiChart) {
        rsiChart.destroy();
    }
    
    // 차트 생성
    rsiChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: Array.from({length: rsiData.length}, (_, i) => i + 1),
            datasets: [{
                label: 'RSI (14)',
                data: rsiData,
                borderColor: 'rgba(153, 102, 255, 1)',
                backgroundColor: 'rgba(153, 102, 255, 0.1)',
                borderWidth: 2,
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    min: 0,
                    max: 100,
                    grid: {
                        color: function(context) {
                            if (context.tick.value === 30 || context.tick.value === 70) {
                                return 'rgba(255, 0, 0, 0.5)';
                            }
                            return 'rgba(0, 0, 0, 0.1)';
                        }
                    }
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const value = context.parsed.y;
                            let status = '';
                            
                            if (value > 70) status = ' (과매수)';
                            else if (value < 30) status = ' (과매도)';
                            
                            return `RSI: ${value.toFixed(2)}${status}`;
                        }
                    }
                }
            }
        }
    });
    
    // 추가 확장: RSI 70, 30 라인 (Chart.js 플러그인 필요)
    if (Chart.annotationLinesDrawn) {
        return;
    }
    
    // 간이 플러그인 방식으로 기준선 추가
    Chart.annotationLinesDrawn = true;
    Chart.defaults.plugins.afterDatasetsDraw = (chart) => {
        const ctx = chart.ctx;
        if (chart.canvas.id === 'rsiChart') {
            const yAxis = chart.scales.y;
            
            // 과매수 (70) 라인
            const y70 = yAxis.getPixelForValue(70);
            ctx.save();
            ctx.beginPath();
            ctx.setLineDash([5, 5]);
            ctx.moveTo(chart.chartArea.left, y70);
            ctx.lineTo(chart.chartArea.right, y70);
            ctx.lineWidth = 1;
            ctx.strokeStyle = 'rgba(255, 0, 0, 0.5)';
            ctx.stroke();
            
            // 과매도 (30) 라인
            const y30 = yAxis.getPixelForValue(30);
            ctx.beginPath();
            ctx.setLineDash([5, 5]);
            ctx.moveTo(chart.chartArea.left, y30);
            ctx.lineTo(chart.chartArea.right, y30);
            ctx.lineWidth = 1;
            ctx.strokeStyle = 'rgba(0, 255, 0, 0.5)';
            ctx.stroke();
            
            // 텍스트 추가
            ctx.font = '12px Arial';
            ctx.fillStyle = 'rgba(255, 0, 0, 0.7)';
            ctx.fillText('과매수 (70)', chart.chartArea.right - 70, y70 - 5);
            
            ctx.fillStyle = 'rgba(0, 255, 0, 0.7)';
            ctx.fillText('과매도 (30)', chart.chartArea.right - 70, y30 + 15);
            
            ctx.restore();
        }
    };
}

/**
 * WebSocket 연결 - 실시간 데이터 수신
 * @param {string} market - 마켓 코드
 * @return {Object} Stomp 클라이언트
 */
function connectWebSocket(market) {
    let socket = new SockJS('/ws');
    let stompClient = Stomp.over(socket);
    
    // 로그 출력 비활성화
    stompClient.debug = null;
    
    stompClient.connect({}, function(frame) {
        console.log('WebSocket 연결 성공');
        
        // 실시간 시세 구독
        stompClient.subscribe('/topic/ticker', function(message) {
            const tickerData = JSON.parse(message.body);
            updateRealTimePrice(tickerData);
        });
        
        // 서버에 실시간 데이터 요청
        stompClient.send("/app/subscribe", {}, JSON.stringify({
            markets: [market]
        }));
    }, function(error) {
        console.error('WebSocket 연결 실패:', error);
    });
    
    return stompClient;
}

/**
 * 실시간 가격 업데이트
 * @param {Object} data - 시세 데이터
 */
function updateRealTimePrice(data) {
    if (!data || !data.code) return;
    
    const market = data.code;
    
    // 현재 선택된 마켓과 일치하는지 확인
    if (market === currentMarket) {
        // 이전 가격 저장
        const oldPrice = realTimePrices[market];
        realTimePrices[market] = data.trade_price;
        
        // 페이지 타이틀 업데이트
        updatePageTitle(market, data.trade_price, data.change_rate);
        
        // 가격 변동 시 알림 (옵션)
        if (oldPrice && Math.abs(data.change_rate) > 0.01) {
            notifyPriceChange(market, data.trade_price, data.change_rate);
        }
    }
}

/**
 * 페이지 타이틀 업데이트
 * @param {string} market - 마켓 코드
 * @param {number} price - 현재 가격
 * @param {number} changeRate - 변동률
 */
function updatePageTitle(market, price, changeRate) {
    const marketName = document.getElementById('marketSelect').options[document.getElementById('marketSelect').selectedIndex].text;
    
    if (price) {
        const direction = changeRate > 0 ? '▲' : (changeRate < 0 ? '▼' : '-');
        const changePercent = Math.abs(changeRate * 100).toFixed(2);
        document.title = `${price.toLocaleString()} ${direction} ${changePercent}% | ${marketName}`;
    } else {
        document.title = `${marketName} - 가상화폐 AI 분석`;
    }
}

/**
 * 가격 변동 알림
 * @param {string} market - 마켓 코드
 * @param {number} price - 현재 가격
 * @param {number} changeRate - 변동률
 */
function notifyPriceChange(market, price, changeRate) {
    const marketName = document.getElementById('marketSelect').options[document.getElementById('marketSelect').selectedIndex].text;
    const direction = changeRate > 0 ? '상승' : '하락';
    const changePercent = (Math.abs(changeRate) * 100).toFixed(2);
    
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(`${marketName} ${direction} 알림`, {
            body: `${changePercent}% ${direction}했습니다. 현재가: ${price.toLocaleString()}`,
            icon: '/favicon.ico'
        });
    }
}

/**
 * 알림 권한 요청
 */
function requestNotificationPermission() {
    if ('Notification' in window) {
        if (Notification.permission !== 'granted' && Notification.permission !== 'denied') {
            Notification.requestPermission();
        }
    }
}

/**
 * 오류 메시지 표시
 * @param {string} message - 오류 메시지
 */
function showError(message) {
    alert(message);
}
